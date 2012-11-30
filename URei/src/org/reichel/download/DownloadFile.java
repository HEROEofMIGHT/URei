package org.reichel.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Properties;

import org.reichel.command.output.Output;
import org.reichel.command.output.SystemOutPrintOutputIntegerImpl;

public class DownloadFile {

	private final String path;
	
	private URL url;
	
	private URLConnection connection;
	
	private final Output<Integer> output;
	
	private Integer fileLength;
	
	private Boolean connected = false;
	
	private String fileName;
	
	public DownloadFile(Output<Integer> output, String path, Charset charset) throws UnsupportedEncodingException{
		this.output = output;
		this.path = URLDecoder.decode(path, charset.name());
	}
	
	public DownloadFile(Output<Integer> output, String filePath) throws UnsupportedEncodingException{
		this(output, filePath, Charset.forName("UTF-8"));
	}
	
	public DownloadFile connect(String fileName){
		if(fileName == null || "".equals(fileName)){
			throw new IllegalArgumentException("fileName n�o pode ser vazio ou nulo.");
		}
		try {
			this.fileName = fileName;
			this.url = new URL(this.path + "/" + fileName);
			this.connection = this.url.openConnection();
			this.connection.setUseCaches(false);
			this.connection.connect();
			this.connected = true;
			this.fileLength = this.connection.getContentLength();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	/**
	 * Aten��o ao utilizar este m�todo a variavel connected n�o representar� o real estado, desde que 
	 * a conex�o fecha assim que o inputStream ou outputStream � fechado atrav�s do m�todo close()
	 * @param fileName caminho do arquivo
	 * @return InputStream com o stream pronto para ser usado
	 * @throws IOException se algum problema ocorrer ao criar o InputStream
	 */
	public InputStream getInputStream(String fileName) throws IOException{
		connect(fileName);
		return this.connection.getInputStream();
	}
	
	public DownloadFile download(String fileName, String targetFolderPath) throws IOException{
		if(this.connected){
			if(!this.fileName.equals(fileName)){
				throw new IllegalArgumentException("fileName: '" + fileName + "' n�o � o mesmo que this.fileName: '" + this.fileName + "' utilize o m�todo connect para atualizar o fileName.");
			}
		} else {
			connect(fileName);
		}
		saveFile(prepareTargetFolder(fileName, targetFolderPath));
		this.connected = false;
		return this;
	}

	public DownloadFile disconnect() throws IOException{
		if(this.connected){
			this.connection.getInputStream().close();
			this.connected = false;
		}
		return this;
	}
	
	public DownloadFile download(String targetFolderPath) throws IOException{
		if(this.connected){
			saveFile(prepareTargetFolder(this.fileName, targetFolderPath));
		}
		return this;
	}
	
	private String prepareTargetFolder(String fileName, String targetFolderPath) {
		String targetFilePath = (targetFolderPath + File.separatorChar + fileName);
		File targetFolder = new File(targetFilePath.substring(0,targetFilePath.lastIndexOf(File.separatorChar)));
		if(!targetFolder.exists()){
			targetFolder.mkdirs();
		}
		return targetFilePath;
	}

	private void saveFile(String targetFilePath) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(new File(targetFilePath));
		InputStream is = this.connection.getInputStream();
		byte[] buffer = new byte[1024];
		Integer bytes;
		while ((bytes = is.read(buffer)) != -1) {
			this.output.output(bytes);
			fos.write(buffer);
		}
		fos.flush();
		fos.close();
		disconnect();
	}
	
	public Integer getFileLength() {
		return fileLength;
	}

	public Boolean getConnected() {
		return connected;
	}
	

	public static void main(String[] args) throws IOException {
		String path = "file:///D:/Documents/Markus/git/URei/URei/target";
		
		DownloadFile downloadFile = new DownloadFile(new SystemOutPrintOutputIntegerImpl(), path);
		Properties properties = new Properties();
		properties.load(downloadFile.getInputStream("files.properties"));
		String targetFolderPath = "target" + File.separatorChar + "download" + File.separatorChar;
		
		for(Entry<Object,Object> entry : properties.entrySet()){
			for(String file : entry.getValue().toString().split(",") ){
				if(!"root".equals(entry.getKey().toString())){
					file = entry.getKey().toString() + File.separatorChar + file;
				}
				downloadFile.download(file, targetFolderPath);
			}
		}
	}
	
}
