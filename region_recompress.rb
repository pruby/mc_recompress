#!/usr/bin/env ruby

# Minecraft region file recompresser
#
# The ability to compress minecraft files is limited by the individual chunks being compressed.
# While this inner compression can eliminate common data within the chunk (e.g. lots of stone),
# the compressed data cannot be further compressed.
# 
# This tool takes DEFLATE encoded chunks in a minecraft region file and re-stores them without
# compression. It stores this by setting the compression mode to "0". Minecraft cannot read
# these archived files, but the tool can convert them back in to a form that it can read.
# 
# The intermediate files are big, but compress better due to the compression being able to find
# common patterns between the chunks. This script immediately concatenates them together in to
# a bzip2 compressed archive, to make the most of the data in common between all the chunks.
# 
# Usage: ruby region_recompress.rb -a input1.mca [input2.mca ...]
# 
# The output files have a ".dec" suffix added to the end. To recreate valid MCA files:
# 
# Usage: ruby region_recompress.rb input1.mca.dec [input2.mca.dec ...]
# 
# This also removes any dead space between stored chunks in a region file.

require 'zlib'
require 'bzip2'

class SimpleMultiFileReader
  def initialize(stream)
    @stream = stream
  end
  
  def each_file
    while !@stream.eof?
      fn_header = @stream.read(4)
      break if fn_header.nil?
      fn_len = fn_header.unpack("N").first
      filename = @stream.read(fn_len)
      data_len = @stream.read(4).unpack("N").first
      data = @stream.read(data_len)
      yield filename, data
    end
  end
end

class SimpleMultiFileWriter
  def initialize(stream)
    @stream = stream
  end
  
  def write_file(filename, data)
    @stream.write([filename.bytesize].pack("N"))
    @stream.write(filename)
    @stream.write([data.bytesize].pack("N"))
    @stream.write(data)
  end
  
  def close
    @stream.close
    @stream = nil
  end
end

class RegionFileRecompressor
  def pad_to_sector(s)
    partial = s.length % 4096
    if partial > 0
      s += "\0" * (4096 - partial)
    end
    s
  end

  def process(data, compressing = true)
    locations = data[0,4096]
    timestamps = data[4096,4096]

    out_data = locations + timestamps
    last_sector = 2 # Start after tables

    1024.times do |chunk_idx|
      location_data = locations[chunk_idx * 4,4]
      timestamp_data = timestamps[chunk_idx * 4, 4]
      
      offset_sectors = ("\0" + location_data[0,3]).unpack('N').first
      length_sectors = location_data[3].unpack('C').first
      
      next if offset_sectors == 0
      
      chunk_header = data[offset_sectors * 4096, 5]
      length = chunk_header[0,4].unpack('N').first
      compression = chunk_header[4].unpack('C').first
      encoded_data = data[offset_sectors * 4096 + 5, length]
      
      if compression == 2 && !compressing
        decompressed_data = Zlib::Inflate.inflate(encoded_data)
        new_length = decompressed_data.length
        new_sectors = (new_length.to_f / 4096).ceil.to_i
        new_chunk_header = [new_length, 0].pack("Nc")
        
        new_chunk = pad_to_sector(new_chunk_header + decompressed_data)
      elsif compression == 0 && compressing
        recompressed_data = Zlib::Deflate.deflate(encoded_data)
        new_length = recompressed_data.length
        new_sectors = (new_length.to_f / 4096).ceil.to_i
        new_chunk_header = [new_length, 2].pack("Nc")
        
        new_chunk = pad_to_sector(new_chunk_header + recompressed_data)
      else
        new_sectors = length_sectors
        new_chunk = pad_to_sector(chunk_header + encoded_data)
      end
      
      new_location_header = [last_sector, new_sectors].pack("Nc")[1,4]
      out_data[chunk_idx * 4, 4] = new_location_header
      out_data << new_chunk
      last_sector += new_sectors
    end
    
    out_data
  end
  
  def create_archive(archive, files)
    num_files = 0
    total_input_size = 0
    Bzip2::Writer.open(archive) do |arfile|
      ar = SimpleMultiFileWriter.new(arfile)
      files.each do |in_file|
        compressing = false
        out_file = in_file + ".dec"
        
        data = nil
        File.open(in_file, 'rb') do |infile|
          data = infile.read
        end
        total_input_size += data.bytesize
        
        out_data = process(data, compressing)
        
        ar.write_file(out_file, out_data)
        
        num_files += 1
      end
    end
    output_size = File.size(archive)
    STDERR.puts "Compressed #{num_files} region files"
    STDERR.puts "Input #{total_input_size} bytes, output #{output_size} bytes"
    STDERR.puts("Compression ratio %0.1f:1" % (total_input_size.to_f / output_size.to_f))
  end
  
  def extract_archive(archive)
    Bzip2::Reader.open(archive) do |arfile|
      ar = SimpleMultiFileReader.new(arfile)
      ar.each_file do |in_file, data|
        compressing = true
        out_file = in_file.gsub(/\.dec$/, '')
        
        out_data = process(data, compressing)
        
        File.open(out_file, 'wb') do |ofile|
          ofile << out_data
        end
      end
    end
  end
end

if $0 == __FILE__
  if ARGV.first == '-a'
    archive = ARGV[1]
    mcas = ARGV[2..-1]
    RegionFileRecompressor.new.create_archive(archive, mcas)
  elsif ARGV.first == '-d'
    archive = ARGV[1]
    RegionFileRecompressor.new.extract_archive(archive)
  end
end
