package org.reichel.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Classe utilit�ria para lidar com arquivos jar.
 * @author Markus Reichel
 * <pre>
 * History: 28/11/2012 - Markus Reichel - Cria��o da classe
 * </pre>
 */
public class JarUtils {

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
	public void extractFiles(String jarFilePath, String targetFolder, boolean extractMetaInf) throws IOException {
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
		JarFile jarFile = new JarFile(new File(jarFilePath));
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
		return jarAttributeVersion != null? new JarVersion(jarAttributeVersion, getFileName(jarFilePath)) : null;
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
		if(jarFile != null){
			Manifest manifest = jarFile.getManifest();
			if(manifest != null){
				Attributes mainAttributes = manifest.getMainAttributes();
				if(mainAttributes != null){
					return mainAttributes.getValue(attribute);
				}
			}
		}
		return null;
	}
	
	private void doExtractFile(JarFile jarFile, JarEntry jarEntry, File targetFile) throws IOException{
		InputStream is = jarFile.getInputStream(jarEntry);
		FileOutputStream fos = new FileOutputStream(targetFile);
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
        	 fos.write(buffer, 0, bytesRead);
        }
        is.close();
        fos.flush();
        fos.close();
	}
	
	private void createDirectories(String targetFolder, File targetFile, JarEntry jarEntry, String name) {
		if(jarEntry.isDirectory() && !targetFile.exists()){
			if(!targetFile.mkdirs()){
				throw new UnsupportedOperationException("N�o foi poss�vel criar diret�rios:'" + targetFile.getAbsolutePath() + "'");
			}
		} else {
			File file = new File(targetFolder + name.substring(0,name.lastIndexOf("/")));
			if(!file.exists()){
				if(!file.mkdirs()){
					throw new UnsupportedOperationException("N�o foi poss�vel criar diret�rios:'" + file.getAbsolutePath() + "'");
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
	 * @param targetFolderPath caminho completo do diret�rio
	 * @param jarVersions instancia de Map&lt;String,JarVersion&gt; a ser populado com as informa��es
	 * @return Map&lt;String,JarVersion&gt com o mapa populado ou vazio se n�o encontrar arquivos jar
	 * @throws IOException caso ocorra problema com a manipula��o de arquivo
	 * @see JarUtils#getJarVersion(String)
	 */
	public Map<String, JarVersion> getJarVersions(String targetFolderPath, Map<String,JarVersion> jarVersions) throws IOException{
		File targetFolderFile = new File(targetFolderPath);
		File file = null;
		if(targetFolderFile.exists()){
			if(targetFolderFile.isDirectory()){
				for(String filePath : targetFolderFile.list()){
					file = new File(targetFolderFile.getAbsolutePath() + File.separatorChar + filePath);
					if(file.exists()){
						 if(file.isDirectory()){
							 getJarVersions(file.getAbsolutePath(), jarVersions);
						 } else if(file.getAbsolutePath().toLowerCase().endsWith(".jar")) {
							 jarVersions.put(file.getName(), getJarVersion(file.getAbsolutePath()));
						 }
					}
				}
			} else { 
				jarVersions.put(targetFolderFile.getName(), getJarVersion(targetFolderFile.getAbsolutePath()));
			}
		}
		return jarVersions;
	}
	
	public Map<String,JarVersion> getJarVersions(String versionEndsWith, String pathEndsWith, Properties properties){
		Map<String,JarVersion> result = new HashMap<String,JarVersion>();
		if(properties == null){
			throw new IllegalArgumentException("Parametro properties n�o pode ser nulo.");
		}
		if(versionEndsWith == null || "".equals(versionEndsWith)){
			throw new IllegalArgumentException("Parametro keyEndsWith n�o pode ser nulo.");
		}
		Enumeration<Object> keys = properties.keys();
		while(keys.hasMoreElements()){
			String key = keys.nextElement().toString();
			if(key.endsWith(versionEndsWith)){
				String fileName = key.replace(versionEndsWith, "");
				String property = properties.getProperty(fileName + pathEndsWith);
				if(property != null){
					property = normalizeFileSeparatorChar(property);
					if(!property.endsWith(Character.toString(File.separatorChar))){
						property += File.separatorChar;
					}
				}
				result.put(fileName + ".jar", new JarVersion(properties.getProperty(key), property + fileName + ".jar"));
			}
		}
		return result;
	}

	public String normalizeFileSeparatorChar(String filePath) {
		return filePath.replace("\\", Character.toString(File.separatorChar)).replace("/", Character.toString(File.separatorChar));
	}
}
