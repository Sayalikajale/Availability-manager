package disaster_recovery;


import java.net.URL;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class PingVm extends Thread{
	HostSystem host;
	PingVm(HostSystem host)
	{
		this.host = host;
		
	}
	public void run()
	{
while(true){
		try {
			
			
			URL url = new URL("https://"+host.getName()+"/sdk");
			ServiceInstance si = new ServiceInstance(url, "root", "12!@qwQW", true);
			Folder rootFolder = si.getRootFolder();
			ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
			
			URL admin_url=new URL("https://130.65.132.113/sdk");
			ServiceInstance si_admin = new ServiceInstance(admin_url, "administrator", "12!@qwQW", true);
			Folder rootFolder_admin = si_admin.getRootFolder();
			ManagedEntity[] mes_admin = new InventoryNavigator(rootFolder_admin).searchManagedEntities("VirtualMachine");
			
			// Get All the VM's of a host
			VirtualMachine vm[] = host.getVms();
			for(int i=0;i < vm.length;i++)
			{	
			
			VM_Functions.vmStatus(vm[i]);
			boolean check=VM_Functions.pingVirtualMachine(vm[i]);
			VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm[i].getRuntime();
			if(check==false)
			{
				
				if(VirtualMachinePowerState.poweredOff == vmri.getPowerState()){
					
				System.out.println("Virtual machine is powered off:"+vm[i].getName()+". Checking for alarms.");
				//check for alarms. if no alarms, create an alarm
				boolean al =VM_Functions.getalarm(si,vm[i]);
				if(al==true) // alarm set
					{
					System.out.println("User might have turned off VM: "+vm[i].getName()+". Cannot ping. Exiting Thread");
					//return ;
					}
					else //alarm not set
					{
					VM_Functions.setalarm(vm[i],si_admin);
					System.out.println("Alarm set for: "+vm[i].getName());
					}
				}
			}
			
			// If VM fails to respond to ping, check host first
			if(VM_Functions.pingVirtualMachine(vm[i])==false){
				
					System.out.println("VM Ping failed. Hence attempting to ping vHost.."+vm[i].getName());
					boolean hostPingStatus=VM_Functions.pingHost(host);
					// If host fails to respond to ping, restart the host thread, so that it can check host health again.
					if(hostPingStatus==false){
						
						System.out.println("Ping failed for VM's in host: "+host.getName()+". Checking host health.");
						HostThread host_thread = new HostThread(host);
						host_thread.start();
						break; // leave this thread, it will be restarted when we start host thread.
					}
					else{ // IF host is alive, then VM is at fault. Revert to its older snapshot
						System.out.println("Reverting VM to prev snapshot for VM: "+vm[i].getName()+" in host: "+host.getName());
						System.out.println("Please wait while snapshots are checked/created...");
						sleep(60000);
						
						boolean rev= VM_Functions.revertToSnapshot(vm[i]);
						if(rev == true)// revert success
						{
							// Revert to previous VM snapshot
							if(VirtualMachinePowerState.poweredOff == vmri.getPowerState())
							{
								VM_Functions.powerOnVM(vm[i]);
								System.out.println("waiting for changes to take effect...");
							}
							System.out.println("Turning on the VM: "+vm[i].getName()+". Please wait.......");
							sleep(180000);
							//sleep(15000);
							boolean ping = VM_Functions.pingVirtualMachine(vm[i]);
							System.out.println("The second ping of VM: "+vm[i].getName()+" is "+VM_Functions.pingVirtualMachine(vm[i]));
							if(ping==false)
							{
								System.out.println("Repair failed.. Something else is wrong with the VM: "+vm[i].getName());
								//return;
							}
								
						}
						else
						{
							System.out.println("No snapshots available to revert.");
						}
						  
				}
					
			}
			else { // VM responds to ping. Everything is fine.
				System.out.println("VM is ON: "+vm[i].getName()+" with ping status="+VM_Functions.pingVirtualMachine(vm[i]));
				}
			}
			System.out.println("PING Thread for: "+host.getName()+" is going to sleep now");
			sleep(300000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// In case of any exception, Close this thread and start Host thread for this particular thread only.
			System.out.println("Ping failed for VM's in host: "+host.getName()+". Checking host health.");
			HostThread host_thread = new HostThread(host);
			host_thread.start();
			break;
		}
}// end of while loop.
	}
}
