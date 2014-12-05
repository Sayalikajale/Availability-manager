package disaster_recovery;

import java.net.MalformedURLException;
import java.net.URL;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class TakeSnapShot extends Thread{
		HostSystem host;
		public TakeSnapShot(){}
		public TakeSnapShot(HostSystem h)
		{
			this.host=h;
		}
		public void run()
		{
while(true){
			try {
				System.out.println("HOST: "+host.getName());
				URL url = new URL("https://"+host.getName()+"/sdk");
				ServiceInstance si = new ServiceInstance(url, "root", "12!@qwQW", true);
				Folder rootFolder = si.getRootFolder();
				ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
				for(int i=0;i<mes.length;i++)
				{
						
						VirtualMachine vm = (VirtualMachine) mes[i];
						System.out.println("Host: "+host.getName().toString());
					
					System.out.println("Taking snapshot of: "+vm.getName());
					vm.removeAllSnapshots_Task();
					vm.createSnapshot_Task("Snapshot Name: "+vm.getName(),"Snapshot for " + vm.getName(), false, false);
					
				}
				System.out.println("Snapshot Thread for: "+host.getName()+" isgoing to sleep now");
				sleep(600000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Snapshots failed for VM's in host: "+host.getName()+". Checking host health.");
				HostThread host_thread = new HostThread(host);
				host_thread.start();
				break;
				
			}
}//end of while loop
		}
}
