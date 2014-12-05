package disaster_recovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class HostThread extends Thread {

	HostSystem host;
	ServiceInstance si,admin_si;
	ManagedEntity[] mes,admin_mes;
	Folder rootFolder,admin_rootFolder;
	
	public HostThread(){}
	public HostThread(HostSystem h)
	{
		this.host=h;
	}
	
	public void run()
	{
		  // This thread is for one host. simultaneously, same thread starts for other hosts
		   try {
			   
			    boolean pingResult=VM_Functions.pingHost(host);
				System.out.println(host.getName()+" : ping result : "+ pingResult);
				System.out.println("CP1 for "+host.getName());
				
				URL urladmin1 = new URL("https://130.65.132.14/sdk");
				ServiceInstance siadmin1 = new ServiceInstance(urladmin1, "administrator", "12!@qwQW", true);
				Folder rootFolderadmin1 = siadmin1.getRootFolder();
		    	ManagedEntity [] mesadmin1 = new InventoryNavigator(rootFolderadmin1).searchManagedEntities("VirtualMachine");
		    
		    	
				if(pingResult==false)
				{						
				    	System.out.println("ping failed for "+host.getName()+". Trying to power it on");
				    	
				    	
				    	int i=1;
				    	while(i<4 && pingResult==false)
				    	{
				    	if(VM_Functions.checkHostPowerState(host)==false)
				    		{
				    		System.out.println("Error with HOST:" +this.host.getName() );
				    		System.out.println("Trying to restore this host.");
				    		System.out.println("Attempt "+i+" of 3");
				    	
							VM_Functions.powerOnHost(mesadmin1, host);
							System.out.println("POWERING ON THE HOST.");
							System.out.println("THIS MIGHT TAKE A LITTLE LONGER. PLEASE WAIT....");
							sleep(120000);
				    		i++;
				    		pingResult =VM_Functions.pingHost(host);
				    		}
				    	}
				    	// Couldn't power on host after 3 attempts
				    	if(pingResult==false){
				    		System.out.println("PING FAILED FOR "+host.getName()+" AFTER TRYING TO POWER ON");
				    		boolean revert_snapshot=VM_Functions.revertHostSnapshot(mesadmin1,host);
				    		
					    	if(revert_snapshot==true)
							{
					    		System.out.println("Reverting to older snapshot for host: "+host.getName());
					    	} else
							{
								System.out.println("Reverting failed for host: "+host.getName());
								System.out.println("Now trying to migrate all VM's for host: "+host.getName());

					    		    // get all LIVE hosts. get number of VM's inside them. migrate VM to host with least VM's.
					    			URL urladmin = new URL("https://130.65.132.113/sdk");
									ServiceInstance siadmin = new ServiceInstance(urladmin, "administrator", "12!@qwQW", true);
									Folder rootFolderadmin = siadmin.getRootFolder();
							    	ManagedEntity [] mesadmin = new InventoryNavigator(rootFolderadmin).searchManagedEntities("HostSystem");
							    	
							    	// Find available hosts and number of VM's inside them
							    	int hosts_counter=0;
							    	int[] number_of_vms = new int[100];
							    	
							    	for(int x=0;x<mesadmin.length;x++)
									{
										 HostSystem hosts=(HostSystem) mesadmin[x];
										 System.out.println("index: "+x+" and host name "+hosts.getName());
										 boolean ping_hosts=VM_Functions.pingHost(hosts);
										 if(ping_hosts==true)
										 {
											 
									         VirtualMachine vms[]=hosts.getVms();
											 number_of_vms[hosts_counter] = (int)vms.length;
											 System.out.println(hosts.getName()+" has: "+number_of_vms[hosts_counter]+" vm's.");
											 hosts_counter++;
										 }
										 else{
											 number_of_vms[hosts_counter] = Integer.MAX_VALUE;
											 System.out.println(hosts.getName()+" has: "+number_of_vms[hosts_counter]+" vm's.");
											 hosts_counter++;
										 }
									}
							    	// if there are no live hosts, then add new host and migrate all to this new host.
							    	
							    	if(hosts_counter<1)
									 {
										 // add new host and then migrate all Vm's of failed host.
							    		 ManagedEntity [] mes_new_host =  new InventoryNavigator(rootFolderadmin).searchManagedEntities("Datacenter");
										 Datacenter dc = new Datacenter(rootFolderadmin.getServerConnection(),  mes_new_host[0].getMOR());
										 HostConnectSpec hs = new HostConnectSpec();
										 String ip= "130.65.133.13";
										 hs.hostName= ip;
										 hs.userName ="root";
										 hs.password = "12!@qwQW";
										 hs.managementIp = "130.65.133.13";
										 hs.setSslThumbprint("C5:EF:CA:98:96:80:6D:2E:46:CB:B1:D2:BB:87:4A:18:AF:26:83:20");
										 //hs.setSslThumbprint("90:BD:8C:C1:4E:F6:E9:A3:1A:DF:4B:FA:16:6B:9A:0D:73:DC:6A:F7");
										 
										 boolean new_host_result = VM_Functions.addNewHost(dc,hs);
										 if(new_host_result==true)
										 {
											 System.out.println("Added new host to the vCenter.");
											// NOW MIGRATE ALL VMS
											    HostSystem newHost = (HostSystem) new InventoryNavigator(rootFolderadmin).searchManagedEntity("HostSystem",ip);
										    	VirtualMachine vm2[]=host.getVms(); // fetchn VM's from old host that is not working.
										    	for(VirtualMachine migrate_vm :vm2){
										    		System.out.println("NOW MIGRATING VIRTUAL MACHINES TO THE NEWLY CREATED HOST");
										    		VM_Functions.MigrateVM(siadmin, migrate_vm, rootFolderadmin,newHost);
										    	}
										 }
										 else
										 {
											 System.out.println("Sorry. Something went wrong. Couldn't add new Host.");
										 }
							    		
							    		
									 }
							    	
							    	// Find host with least number of VM's
							    	int small = Integer.MAX_VALUE;
							    	int index_identifier=0;
							    	int index = 0;
							    	for (int indexer : number_of_vms)
							    	{
							    		System.out.println("NUMBER OF VMS: "+indexer);
							    		if(indexer < small){
							                small = indexer;
							                
							                index = index_identifier;
							    		}  
							                index_identifier++;
							            
							    	}
							    	// migrate all VM's in the host mesadmin[index] with index= index of host with least VM's
							    	
							    	HostSystem newhost=(HostSystem) mesadmin[index];
							    	VirtualMachine vm2[]=host.getVms(); // fetchn VM's from old host that is not working.
							    	for(VirtualMachine migrate_vm :vm2){
							    		System.out.println("NOW MIGRATING VIRTUAL MACHINES TO A SAFE HOST");
							    		VM_Functions.MigrateVM(siadmin, migrate_vm, rootFolderadmin,newhost);
							    	}
							}
				    		
				    	}
				}
			// If host is alive, then we will check VM's inside host by creating a thread for ping and snapshot process of a VM
		   if(pingResult==true){
				
			   
				boolean take_host_snap=VM_Functions.takeHostSnapshot(mesadmin1,host);
				
		    	if(take_host_snap==false)
				{
		    		System.out.println("Something went wrong. Couldn't take snapshot for host: "+host.getName());
				} else
				{
					System.out.println("Snapshot taken for host: "+host.getName());
				}
				
				//Threads for each VM starts
				TakeSnapShot snapshots=new TakeSnapShot(host);
				PingVm pingVm = new PingVm(host);
				while(true){
					
				snapshots.start();
				sleep(5000);
				pingVm.start();
		
				sleep(720000);
				}
		   }
		}
	        catch ( Exception e ) 
	        { 
	        	System.out.println( e.toString() ) ;
	        }
			
	}
}
