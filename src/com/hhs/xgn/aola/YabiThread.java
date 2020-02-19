package com.hhs.xgn.aola;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
public class YabiThread extends Thread {
	int id;
	String format;
	boolean cwork;
	boolean cdecom;
	
	public void log(String str){
		System.out.println("[Thread#"+id+"]"+str);
	}
	public YabiThread(int id,String format,boolean cwork,boolean cdecom){
		this.id=id;
		this.format=format;
		this.cwork=cwork;
		this.cdecom=cdecom;
		log("Init success!");
	}

	public String inner(String s){
		String ans="";
		int x=0;
		for(int i=0;i<s.length();i++){
			if(s.charAt(i)=='<'){
				x++;
			}else if(s.charAt(i)=='>'){
				x--;
			}else if(x==0){
				ans+=s.charAt(i);
			}
		}
		return ans.trim();
	}
	
	File folder;
	
	/**
	 * Copy a file
	 * 
	 * @param fromFile
	 * @param toFile
	 *            <br/>
	 *            
	 * @throws IOException
	 */
	public void copyFile(File fromFile, File toFile) throws IOException {
		FileInputStream ins = new FileInputStream(fromFile);
		FileOutputStream out = new FileOutputStream(toFile);
		byte[] b = new byte[1024];
		int n = 0;
		while ((n = ins.read(b)) != -1) {
			out.write(b, 0, n);
		}

		ins.close();
		out.close();
	}
	
	public void run(){
		try{
			//create the folder
			folder=new File("yabbi_"+id);
			if(!folder.exists()){
				folder.mkdirs();
			}
			
			if(!cwork){
				
				
				//download the pack
				log("Start downloading the swf");
				URL ur=new URL("http://aola.100bt.com/play/fightassets/pet"+id+".swf");
				InputStream is=ur.openStream();
				BufferedInputStream bis=new BufferedInputStream(is);
				BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(folder+"/pet.swf"));
				byte[] by=new byte[1024*1024];
				int len;
				while((len=bis.read(by))!=-1){
					bos.write(by, 0, len);
					bos.flush();
				}
				
				bos.close();
				bis.close();
				
				//call xml
				log("Start generating xml");
				ProcessBuilder pb=new ProcessBuilder("java","-jar","../ffdec.jar","-swf2xml","pet.swf","pet.xml");
				pb.directory(folder);
				pb.redirectOutput(new File(folder+"/ffdec.log"));
				pb.redirectError(new File(folder+"/ffdec.log"));
				
				Process p=pb.start();
				p.waitFor();
				int ex=p.exitValue();
				if(ex!=0){
					throw new Exception("FFDEC Failure. See ffdec.log");
				}
			}
			
			Process p2=null;
			if(!cdecom){
				//decompress data
				log("Start decompressing swf. This may take a while");
				ProcessBuilder pb2=new ProcessBuilder("java","-jar","../ffdec.jar","-format","sprite:"+format,"-export","sprite","temp","pet.swf");
				pb2.directory(folder);
				pb2.redirectOutput(new File(folder+"/ffdec2.log"));
				pb2.redirectError(new File(folder+"/ffdec2.log"));
				p2=pb2.start();
			}
			
			//while waiting for process
			//parse xml
			log("Parsing XML");
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(folder+"/pet.xml")));
			String line;
			
			String s1=null,s2=null;
			while((line=br.readLine())!=null){
				if(line.contains("<item type=\"SymbolClassTag\">")){
					br.readLine(); //<tags>
					s1=inner(br.readLine());
					s2=inner(br.readLine());
					break;
				}
			}
	        br.close();
	        
			log("Find out core element: front="+s1+" end="+s2);
			
			ArrayList<String> front=findDepend(s1);
			ArrayList<String> back=findDepend(s2);
			
			if(!cdecom){
				//continue to wait
				log("Waiting for thread to end");
				p2.waitFor();
				int ex2=p2.exitValue();
				if(ex2!=0){
					throw new Exception("FFDEC Failure. See ffdec.log");
				}
			}
			
			//collect
			log("Collecting files");
			
			File out=new File("output/"+id+"/front");
			if(!out.exists()){
				out.mkdirs();
			}
			
			for(String x:front){
				if(format.equals("gif")){
					copyFile(folder+"/temp/DefineSprite_"+x+"/frames.gif",out+"/"+x+".gif");
				}else{
					copyFile(folder+"/temp/DefineSprite_"+x+"/1.svg",out+"/"+x+".svg");
				}
			}
			
			File out2=new File("output/"+id+"/back");
			if(!out2.exists()){
				out2.mkdirs();
			}
			
			for(String x:back){
				if(format.equals("gif")){
					copyFile(folder+"/temp/DefineSprite_"+x+"/frames.gif",out2+"/"+x+".gif");
				}else{
					copyFile(folder+"/temp/DefineSprite_"+x+"/1.svg",out2+"/"+x+".svg");
				}
			}
			
			log("Gracefully exit!");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void copyFile(String string, String string2) throws IOException {
		log("Copy from "+string+" -> "+string2);
		copyFile(new File(string),new File(string2));
	}
	
	public ArrayList<String> findDepend(String s1) throws IOException {
		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(folder+"/pet.xml")));
		String line;
		
		ArrayList<String> arr=new ArrayList<>();
		while((line=br.readLine())!=null){
			if(line.contains("spriteId=\""+s1+"\"")){
				String c="<item characterId=\"";
				while((line=br.readLine())!=null){
					if(line.contains("</subTags>")){
						break;
					}else if(line.contains(c)){
						int pos1=line.indexOf(c)+c.length();
						int pos2=line.indexOf('"',pos1);
						arr.add(line.substring(pos1,pos2));
					}
				}
			}
		}
        br.close();
        
        log("Find related sprite for "+s1+":"+arr);
        
        return arr;
	}
}
