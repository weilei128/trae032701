package com.account.util;

import com.account.config.CsvProperties;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * CSV文件存储工具类
 */
@Component
public class CsvStorageUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvStorageUtil.class);
    
    private final CsvProperties csvProperties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public CsvStorageUtil(CsvProperties csvProperties) {
        this.csvProperties = csvProperties;
    }
    
    @PostConstruct
    public void init() {
        File dataDir = new File(csvProperties.getDataDir());
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                logger.info("创建数据目录: {}", dataDir.getAbsolutePath());
            }
        }
        
        initFile(csvProperties.getUsersFile(), new String[]{"id", "username", "password", "nickname", "createTime"});
        initFile(csvProperties.getRecordsFile(), new String[]{"id", "userId", "amount", "type", "category", "remark", "createTime"});
    }
    
    private void initFile(String fileName, String[] headers) {
        File file = new File(csvProperties.getDataDir(), fileName);
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    writeLine(fileName, headers);
                    logger.info("创建CSV文件: {}", file.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("创建CSV文件失败: {}", fileName, e);
            }
        }
    }
    
    private File getFile(String fileName) {
        return new File(csvProperties.getDataDir(), fileName);
    }
    
    /**
     * 读取所有行
     */
    public List<String[]> readAll(String fileName) {
        lock.readLock().lock();
        try {
            File file = getFile(fileName);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            List<String[]> result = new ArrayList<>();
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] line;
                boolean isFirst = true;
                while ((line = reader.readNext()) != null) {
                    if (isFirst) {
                        isFirst = false;
                        continue;
                    }
                    result.add(line);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("读取CSV文件失败: {}", fileName, e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 写入一行
     */
    public void writeLine(String fileName, String[] data) {
        lock.writeLock().lock();
        try {
            File file = getFile(fileName);
            try (CSVWriter writer = new CSVWriter(new FileWriter(file, true))) {
                writer.writeNext(data);
            }
        } catch (Exception e) {
            logger.error("写入CSV文件失败: {}", fileName, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 覆盖写入所有行
     */
    public void writeAll(String fileName, String[] headers, List<String[]> dataList) {
        lock.writeLock().lock();
        try {
            File file = getFile(fileName);
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                writer.writeNext(headers);
                for (String[] data : dataList) {
                    writer.writeNext(data);
                }
            }
        } catch (Exception e) {
            logger.error("覆盖写入CSV文件失败: {}", fileName, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 更新一行
     */
    public boolean updateLine(String fileName, int idIndex, String id, String[] newData) {
        lock.writeLock().lock();
        try {
            List<String[]> allLines = readAll(fileName);
            boolean found = false;
            for (int i = 0; i < allLines.size(); i++) {
                if (allLines.get(i)[idIndex].equals(id)) {
                    allLines.set(i, newData);
                    found = true;
                    break;
                }
            }
            
            if (found) {
                String[] headers = getHeaders(fileName);
                writeAll(fileName, headers, allLines);
            }
            return found;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 删除一行
     */
    public boolean deleteLine(String fileName, int idIndex, String id) {
        lock.writeLock().lock();
        try {
            List<String[]> allLines = readAll(fileName);
            boolean removed = allLines.removeIf(line -> line[idIndex].equals(id));
            
            if (removed) {
                String[] headers = getHeaders(fileName);
                writeAll(fileName, headers, allLines);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 根据ID查找一行
     */
    public String[] findById(String fileName, int idIndex, String id) {
        List<String[]> allLines = readAll(fileName);
        for (String[] line : allLines) {
            if (line[idIndex].equals(id)) {
                return line;
            }
        }
        return null;
    }
    
    /**
     * 根据条件查找多行
     */
    public List<String[]> findByCondition(String fileName, int columnIndex, String value) {
        List<String[]> allLines = readAll(fileName);
        List<String[]> result = new ArrayList<>();
        for (String[] line : allLines) {
            if (line[columnIndex].equals(value)) {
                result.add(line);
            }
        }
        return result;
    }
    
    private String[] getHeaders(String fileName) {
        if (fileName.equals(csvProperties.getUsersFile())) {
            return new String[]{"id", "username", "password", "nickname", "createTime"};
        } else if (fileName.equals(csvProperties.getRecordsFile())) {
            return new String[]{"id", "userId", "amount", "type", "category", "remark", "createTime"};
        }
        return new String[]{};
    }
    
    /**
     * 生成唯一ID
     */
    public String generateId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
