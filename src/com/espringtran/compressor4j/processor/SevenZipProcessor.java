/*
 * Copyright (C) 2013-2015 E-Spring Tran
 * 
 *             https://espringtran.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.espringtran.compressor4j.processor;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import com.espringtran.compressor4j.bean.BinaryFile;
import com.espringtran.compressor4j.compressor.FileCompressor;
import com.espringtran.compressor4j.util.FileUtil;
import com.espringtran.compressor4j.util.LogUtil;

/**
 * 
 * @author E-Spring Tran
 * 
 */
public class SevenZipProcessor implements CompressProcessor {

    /**
     * Compress data
     * 
     * @param fileCompressor
     *            FileCompressor object
     * @return
     * @throws Exception
     */
    @Override
    public byte[] compressData(FileCompressor fileCompressor) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SevenZOutputFile sevenZOutput = new SevenZOutputFile(new File(
                fileCompressor.getCompressedPath()));
        try {
            for (BinaryFile binaryFile : fileCompressor.getMapBinaryFile()
                    .values()) {
                SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(
                        new File(binaryFile.getSrcPath()),
                        binaryFile.getDesPath());
                entry.setSize(binaryFile.getActualSize());
                sevenZOutput.putArchiveEntry(entry);
                sevenZOutput.write(binaryFile.getData());
                sevenZOutput.closeArchiveEntry();
            }
            sevenZOutput.finish();
        } catch (Exception e) {
            FileCompressor.LOGGER.error("Error on compress data", e);
        } finally {
            sevenZOutput.close();
            baos.close();
        }
        return baos.toByteArray();
    }

    /**
     * Read from compressed file
     * 
     * @param srcPath
     *            path of compressed file
     * @param fileCompressor
     *            FileCompressor object
     * @throws Exception
     */
    @Override
    public void read(String srcPath, FileCompressor fileCompressor)
            throws Exception {
        long t1 = System.currentTimeMillis();
        byte[] data = FileUtil.convertFileToByte(srcPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SevenZFile sevenZFile = new SevenZFile(new File(srcPath));
        try {
            byte[] buffer = new byte[1024];
            int readByte;
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null && entry.getSize() > 0) {
                long t2 = System.currentTimeMillis();
                baos = new ByteArrayOutputStream();
                readByte = sevenZFile.read(buffer);
                while (readByte != -1) {
                    baos.write(buffer, 0, readByte);
                    readByte = sevenZFile.read(buffer);
                }
                BinaryFile binaryFile = new BinaryFile(entry.getName(),
                        baos.toByteArray());
                fileCompressor.addBinaryFile(binaryFile);
                LogUtil.createAddFileLog(fileCompressor, binaryFile, t2,
                        System.currentTimeMillis());
                entry = sevenZFile.getNextEntry();
            }
        } catch (Exception e) {
            FileCompressor.LOGGER.error("Error on get compressor file", e);
        } finally {
            sevenZFile.close();
            baos.close();
        }
        LogUtil.createReadLog(fileCompressor, srcPath, data.length, t1,
                System.currentTimeMillis());
    }

}
