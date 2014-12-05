package disaster_recovery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class MainClass {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		URL url = new URL("https://130.65.132.113/sdk");
		ServiceInstance si = new ServiceInstance(url, "administrator", "12!@qwQW", true);
		Folder rootFolder = si.getRootFolder();
		String name = rootFolder.getName();
		
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
		
		
		for(int i=0;i<mes.length;i++)
		{
			HostSystem host=(HostSystem) mes[i];
			//Start one thread per host
			HostThread host_thread = new HostThread(host);
			host_thread.start();
		}
		
		// if there are no hosts in vCenter, then create a host.
		if(mes.length==0){
			
			
			 ManagedEntity [] mes_new_host =  new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter");
			 Datacenter dc = new Datacenter(rootFolder.getServerConnection(),  mes_new_host[0].getMOR());
			 HostConnectSpec hs = new HostConnectSpec();
			 String ip= "130.65.133.12";
			 hs.hostName= ip;
			 hs.userName ="root";
			 hs.password = "12!@qwQW";
			 hs.managementIp = "130.65.133.12";
			 hs.setSslThumbprint("C5:EF:CA:98:96:80:6D:2E:46:CB:B1:D2:BB:87:4A:18:AF:26:83:20");
			 //hs.setSslThumbprint("90:BD:8C:C1:4E:F6:E9:A3:1A:DF:4B:FA:16:6B:9A:0D:73:DC:6A:F7");
			 
			 boolean new_host_result = VM_Functions.addNewHost(dc,hs);
			 if(new_host_result==true)
			 {
				 System.out.println("Added new host to the vCenter.");
			 }
			 else
			 {
				 System.out.println("Sorry. Something went wrong. Couldn't add new Host.");
			 }
			
		}
		
	}
		

}
