package org.reichel.jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

/**
 * Classe utilit�ria para lidar com arquivos jar.
 * @author Markus Reichel
 * <pre>
 * History: 28/11/2012 - Markus Reichel - Cria��o da classe
 * </pre>
 */
public class JarUtils {

	private static final Logger logger = Logger.getLogger(JarUtils.class);
	
	public static String PROPERTIES_VERSION = ".version";
	public static String PROPERTIES_FILENAME = ".filename";
	public static String PROPERTIES_PATH = ".path";
	public static String PROPERTIES_TYPE = ".type";
	
	/**
	 * Extrai todos os arquivos de um arquivo jar para um diret�rio.
	 * Exemplo de utiliza��o para extrair todos os arquivos:
	 * <pre>
	 *  try {
     *    JarUtils.extractFiles("target/URei.jar", "target/extract");
	 *  } catch (IOException e) {
	 *    System.out.println(e.getMessage());
	 *  }
	 * </pre>
	 * @param jarFilePath caminho do arquivo jar a ser extra�do ex: config\ambienteconfig.jar
	 * @param targetFolder caminho do diret�rio raiz onde os arquivos ser�o extra�dos ex: config\extract
	 * @throws IOException quando houver problemas ao ler e/ou escrever arquivos
	 */
	public void extractFiles(String jarFilePath, String targetFolder) throws IOException {
		extractFiles(jarFilePath, targetFolder, true);
	}

	/**
	 * Extrai todos os arquivos de um arquivo jar para um diret�rio.
	 * Caso o parametro extractMetaInf for false TODOS os caminhos de arquivo que contiver o nome META-INF n�o ser�o extra�dos.
	 * Exemplo de utiliza��o para extrair todos os arquivos:
	 * <pre>
	 *  try {
     *    JarUtils.extractFiles("target/URei.jar", "target/extract");
	 *  } catch (IOException e) {
	 *    System.out.println(e.getMessage());
	 *  }
	 * </pre>
	 * Exemplo de utiliza��o para extrair tudo menos o diret�rio META-INF:
	 * <pre>
	 *  try {
     *    JarUtils.extractFiles("target/URei.jar", "target/extract", false);
	 *  } catch (IOException e) {
	 *    System.out.println(e.getMessage());
	 *  }
	 * </pre>
	 * 
	 * @param jarFilePath caminho do arquivo jar a ser extra�do ex: config\ambienteconfig.jar
	 * @param targetFolder caminho do diret�rio raiz onde os arquivos ser�o extra�dos ex: config\extract
	 * @throws IOException quando houver problemas ao ler e/ou escrever arquivos
	 */
	public void extractFiles(String jarFilePath, String targetFolder, boolean extractMetaInf) {
		if(jarFilePath == null || "".equals(jarFilePath)){
			throw new IllegalArgumentException("Parametro jarFilePath n�o pode ser vazio ou nulo.");
		}
		if(targetFolder == null){
			throw new IllegalArgumentException("Parametro jarFilePath n�o pode ser nulo.");
		}
		
		if(!targetFolder.endsWith(Character.toString(File.separatorChar))){
			targetFolder += File.separatorChar;
		}
		
		File targetFile = null;
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(new File(jarFilePath));
		} catch (IOException e) {
			logger.error("Problemas ao criar JarFile: " + jarFilePath + " " + e.getMessage());
		}
		if(jarFile != null){
			Enumeration<JarEntry> jarEntries = jarFile.entries();
			while(jarEntries.hasMoreElements()){
				JarEntry jarEntry = jarEntries.nextElement();
				String name = jarEntry.getName();
				if(!name.contains("META-INF") || extractMetaInf){
					targetFile = new File(targetFolder + name);
					createDirectories(targetFolder, targetFile, jarEntry, name);
					doExtractFile(jarFile, jarEntry, targetFile);
				}
			}
		}
		if(jarFile != null){
			try {
				jarFile.close();
			} catch (IOException e) {
				logger.error("Problemas ao liberar recursos: " + e.getMessage());
			}
		}
	}

	/**
	 * M�todo para facilitar a recuperar a vers�o de um jar.
	 * @param jarFilePath caminho do arquivo jar
	 * @return JarVersion objeto com a vers�o do jar ou null se n�o encontrar o atributo 'Implementation-Version' no MANIFEST.MF
	 * @throws IOException se algum problema ocorrer ao ler o arquivo jar.
	 */
	public JarVersion getJarVersion(String jarFilePath) throws IOException{
		if(jarFilePath == null || "".equals(jarFilePath)){
			throw new IllegalArgumentException("Parametro jarFilePath n�o pode ser nulo.");
		}
		String jarAttributeVersion = getJarAttribute(jarFilePath, "Implementation-Version");
		String jarType = getJarAttribute(jarFilePath, "Jar-Type");
		JarTypeEnum jarTypeEnum = (jarType == null || "".equals(jarType))? JarTypeEnum.JAR : JarTypeEnum.fromType(jarType);
		return jarAttributeVersion != null? new JarVersion(jarAttributeVersion, getFileName(jarFilePath), jarTypeEnum) : null;
	}

	public JarVersion getJarVersion(String rootFolder, String jarFilePath) throws IOException{
		if(jarFilePath == null || "".equals(jarFilePath)){
			throw new IllegalArgumentException("Parametro jarFilePath n�o pode ser nulo.");
		}
		if(rootFolder == null || "".equals(rootFolder)){
			throw new IllegalArgumentException("Parametro rootFolder n�o pode ser nulo.");
		}
		
		String relativeJarFilePath = jarFilePath.substring(rootFolder.length() + 1);
		String jarAttributeVersion = getJarAttribute(jarFilePath, "Implementation-Version");
		String jarType = getJarAttribute(jarFilePath, "Jar-Type");
		JarTypeEnum jarTypeEnum = (jarType == null || "".equals(jarType))? JarTypeEnum.JAR : JarTypeEnum.fromType(jarType);
		return jarAttributeVersion != null? new JarVersion(jarAttributeVersion, relativeJarFilePath, jarTypeEnum) : null;
	}
	
	private String getFileName(String filePath) {
		if(filePath == null || "".equals(filePath)){
			throw new IllegalArgumentException("Parametro filePath n�o pode ser nulo.");
		}
		return filePath.lastIndexOf("\\") == -1 ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath.substring(filePath.lastIndexOf("\\") + 1);
	}

	/**
	 * M�todo para facilitar a recuperar atributos principais do arquivo MANIFEST.MF de um arquivo jar.
	 * @param jarFilePath caminho do arquivo jar
	 * @param attribute nome do atributo desejado
	 * @return String com o atributo ou null se n�o encontrar o atributo.
	 * @throws IOException se algum problema ocorrer ao ler o arquivo jar.
	 */
	public String getJarAttribute(String jarFilePath, String attribute) throws IOException{
		JarFile jarFile = new JarFile(jarFilePath);
		String attributeValue = null;
		if(jarFile != null){
			Manifest manifest = jarFile.getManifest();
			if(manifest != null){
				Attributes mainAttributes = manifest.getMainAttributes();
				if(mainAttributes != null){
					attributeValue = mainAttributes.getValue(attribute);
				}
				if(attributeValue == null){
					for(Entry<String, Attributes> att : jarFile.getManifest().getEntries().entrySet()){
						if((attributeValue = att.getValue().getValue(attribute)) != null){
							break;
						}
					}
				}
			}
		}
		return attributeValue;
	}
	
	private void doExtractFile(JarFile jarFile, JarEntry jarEntry, File targetFile) {
		InputStream is = null;
		try {
			is = jarFile.getInputStream(jarEntry);
		} catch (IOException e) {
			logger.error("Erro ao pegar inputStream de: " + jarFile.getName() + " " + e.getMessage());
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetFile);
		} catch (FileNotFoundException e) {
			logger.error("Erro ao criar FileOutputStream: " + targetFile + " " + e.getMessage());
		}
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		if(is != null && fos != null){
	        try {
				while ((bytesRead = is.read(buffer)) != -1) {
					 fos.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				logger.error("Erro ao ler do inputStream ou escrever no FileOutputStream: " + e.getMessage());
			}
		}
		if(is != null && fos != null){
	        try {
				is.close();
				fos.flush();
				fos.close();
			} catch (IOException e) {
				logger.error("Problemas ao liberar recursos: " + e.getMessage());
			}
		}
	}
	
	private void createDirectories(String targetFolder, File targetFile, JarEntry jarEntry, String name) {
		if(jarEntry.isDirectory() && !targetFile.exists()){
			if(!targetFile.mkdirs()){
				throw new UnsupportedOperationException("N�o foi poss�vel criar diret�rios:'" + targetFile.getAbsolutePath() + "'");
			}
		} else if(name != null){
			name = normalizeFileSeparatorChar(name);
			String path;
			if(name.lastIndexOf(File.separatorChar) != -1){
				path = targetFolder + name.substring(0,name.lastIndexOf(File.separatorChar));	
				File file = new File(path);
				if(!file.exists()){
					if(!file.mkdirs()){
						throw new UnsupportedOperationException("N�o foi poss�vel criar diret�rios:'" + file.getAbsolutePath() + "'");
					}
				}
			}
		}
	}
	
	/**
	 * Varre o diret�rio e os sub-diret�rios recursivamente em busca de arquivos jar
	 * populando o mapa com o seu caminho completo com sua respectiva vers�o.
	 * Exemplo de utiliza��o:
	 * <pre>
	 *   String path="C:\\work\\desenv\\git\\URei\\URei\\target";
	 *   Map<String, JarVersion> jarVersions = JarUtils.getJarVersions(path, new HashMap&lt;String, JarVersion&gt;());
	 *   for(Entry<String,JarVersion> jarVersion : jarVersions.entrySet()){
	 *     System.out.println(jarVersion.getKey() + " : " + jarVersion.getValue());
	 *   }
	 * </pre>
	 * @param rootFolder caminho completo do diret�rio
	 * @param jarVersions instancia de Map&lt;String,JarVersion&gt; a ser populado com as informa��es
	 * @return Map&lt;String,JarVersion&gt com o mapa populado ou vazio se n�o encontrar arquivos jar
	 * @throws IOException caso ocorra problema com a manipula��o de arquivo
	 * @see JarUtils#getJarVersion(String)
	 */
	public Map<String, JarVersion> getJarVersions(String rootFolder, Map<String,JarVersion> jarVersions) throws IOException{
		return getJarVersions(rootFolder, rootFolder, jarVersions, (String[]) null);
	}

	public Map<String, JarVersion> getJarVersions(String rootFolder, Map<String,JarVersion> jarVersions, String ... exceptions) throws IOException{
		return getJarVersions(rootFolder, rootFolder, jarVersions, exceptions);
	}
	
	private Map<String, JarVersion> getJarVersions(String rootFolder, String targetFolder, Map<String,JarVersion> jarVersions, String... exceptions) throws IOException{
		File targetFolderFile = new File(targetFolder);
		File file = null;
		if(targetFolderFile.exists()){
			if(targetFolderFile.isDirectory()){
				if(exceptions == null || exceptions.length == 0 || !isException(targetFolderFile.getAbsolutePath(), exceptions)){
					for(String filePath : targetFolderFile.list()){
						file = new File(targetFolderFile.getAbsolutePath() + File.separatorChar + filePath);
						if(file.exists()){
							 if(file.isDirectory()){
								 getJarVersions(rootFolder, file.getAbsolutePath(), jarVersions, exceptions);
							 } else if(file.getAbsolutePath().toLowerCase().endsWith(".jar")) {
								 jarVersions.put(file.getAbsolutePath().substring(rootFolder.length() + 1), getJarVersion(rootFolder, file.getAbsolutePath()));
							 }
						}
					}
				}
			} else if(targetFolderFile.getAbsolutePath().toLowerCase().endsWith(".jar")){ 
				jarVersions.put(targetFolderFile.getAbsolutePath().substring(rootFolder.length() + 1), getJarVersion(rootFolder, targetFolderFile.getAbsolutePath()));
			}
		}
		return jarVersions;
	}
	
	private boolean isException(String absolutePath, String[] exceptions) {
		for(String except : exceptions){
			if(except != null && !"".equals(except) && absolutePath.endsWith(except)){
				return true;
			}
		}
		return false;
	}

	public Map<String,JarVersion> getJarVersions(Properties properties){
		Map<String,JarVersion> result = new HashMap<String,JarVersion>();
		if(properties == null){
			throw new IllegalArgumentException("Parametro properties n�o pode ser nulo.");
		}
		Enumeration<Object> keys = properties.keys();
		String keyRoot = "";
		while(keys.hasMoreElements()){
			String key = keys.nextElement().toString();
			if(key.endsWith(PROPERTIES_VERSION)){
				keyRoot = key.replace(PROPERTIES_VERSION, ""); 
				String fileName = properties.getProperty(keyRoot + PROPERTIES_FILENAME);
				String relativePath = properties.getProperty(keyRoot + PROPERTIES_PATH);
				if(relativePath != null){
					relativePath = normalizeFileSeparatorChar(relativePath);
					if(!"".equals(relativePath) && !relativePath.endsWith(Character.toString(File.separatorChar))){
						relativePath += File.separatorChar;
					}
				}
				result.put(relativePath + fileName, new JarVersion(properties.getProperty(key), relativePath + fileName, JarTypeEnum.fromType(properties.getProperty(keyRoot + PROPERTIES_TYPE))));
			}
		}
		return result;
	}

	public String normalizeFileSeparatorChar(String filePath) {
		return filePath.replace("\\", Character.toString(File.separatorChar)).replace("/", Character.toString(File.separatorChar));
	}

	public static void main(String[] args) throws IOException {
		new JarUtils().extractFiles("C:\\work\\desenv\\git\\AmbienteConfig\\AmbienteConfig\\target\\ambiente-config.jar", "C:\\work\\desenv\\git\\AmbienteConfig\\AmbienteConfig\\target\\x");
	}
}
