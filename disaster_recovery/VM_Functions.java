package disaster_recovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import com.vmware.vim25.Action;
import com.vmware.vim25.AlarmAction;
import com.vmware.vim25.AlarmSetting;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmState;
import com.vmware.vim25.AlarmTriggeringAction;
import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.GroupAlarmAction;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemPowerState;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.MethodActionArgument;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SendEmailAction;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;


public class VM_Functions {
	
	public static boolean pingHost(HostSystem host) throws Exception{
   		   boolean pingResult=false;
     	   try {
			String consoleResult="";
			   System.out.println("Pinging host: "+host.getName());
			   String pingCmd = "ping " + host.getName();

				Runtime r = Runtime.getRuntime();
				Process p = r.exec(pingCmd);

				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String inputLine;
				
				while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				consoleResult+=inputLine;
				}
				if(consoleResult.contains("Connection timed out")||consoleResult.contains("Request timed out"))
				{
					System.out.println("Packets Dropped");
					pingResult=false;
					//flag=false;
				}
				else
				{
					//ping successful
					System.out.println("ping success in vhost: "+host.getName());
					pingResult=true;
					
				}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
			return pingResult;
	}
	
	public static Boolean checkHostPowerState(HostSystem host)
	{
		Boolean flag = true;
		HostRuntimeInfo hri=host.getRuntime();
		String state=hri.getPowerState().toString();
		
		if(hri.getPowerState() == HostSystemPowerState.poweredOff)
		{
			System.out.println("HOST "+host.getName() +" has a state: " +state);
			flag = false;
		}
		if(hri.getPowerState()== HostSystemPowerState.unknown)
		{
			System.out.println("HOST "+host.getName() +" has a state: " +state);
	    	flag = false;
		}
	
		return flag;
	}
	
	public static void powerOnHost( ManagedEntity mes[], HostSystem host) throws Exception
	{
		String str = host.getName().toString();
		String str1 = str.substring(7);
		
		for (int i =0;i < mes.length; i ++)
		{
		VirtualMachine vm= (VirtualMachine)mes[i];
			String vmName = vm.getName();
			if (vmName.contains(str1))
			{
				powerOnVM(vm);
				return;
			}
		}
	}
	
	public static void powerOnVM(VirtualMachine vm) throws Exception
	{
		try{
		Task task = vm.powerOnVM_Task(null);
		String status = task.waitForMe();
	    if(status==Task.SUCCESS)
	    {
	      System.out.println(vm.getName() + " VM Powered ON Successfully.");
	    }
	    else
	    {
	      System.out.println("Failure to power ON");
	    }
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public static void vmStatus(VirtualMachine vm) throws Exception{
		System.out.println("*****************************************");
		System.out.println(vm.getName()+" is Alive");
		System.out.println("IP address of VM: "+vm.getGuest().getIpAddress());
		System.out.println("CPU Usage of Virtual Machine "+vm.getSummary().getQuickStats().overallCpuUsage);
		System.out.println("Memory Usage of Virtual Machine "+vm.getSummary().getQuickStats().getGuestMemoryUsage());
		System.out.println(vm.getNetworks());
		System.out.println("*****************************************");
	}
	
	// Ping VM
	public static boolean pingVirtualMachine(VirtualMachine vm) throws Exception{
		   boolean pingResult=false;
  	       String consoleResult="";
		   System.out.println("Thread started for: "+vm.getName());
		   System.out.println("*** "+vm.getName()+" its ip address is: "+vm.getGuest().getIpAddress()+" ***");
		   
		   //before pinging check if IP address is available
		   if(vm.getGuest().getIpAddress()==null){
			   //System.out.println("CHECKING IF NULL IP IS RETURNED AS FALSE");
			   pingResult=false;
			   return pingResult;
		   }
		   
		    String pingCmd = "ping " + vm.getGuest().getIpAddress();

			Runtime r = Runtime.getRuntime();
			Process p = r.exec(pingCmd);

			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) {
			consoleResult+=inputLine;
			}
			if(consoleResult.contains("Request timed out"))
			{
				System.out.println("Packets Dropped");
				pingResult=false;
			}
			else
			{
				//ping successful
				System.out.println("ping success in vm");
				pingResult=true;
				
			}
			
			return pingResult;
	}
	
	//// POWER STATE OF A VM ///
	public static boolean  stateOfVM(VirtualMachine vm)
    {
    	boolean res=false;
    	
    	try{
    	VirtualMachineRuntimeInfo vmri=vm.getRuntime();
    	//String state=vmri.getPowerState().toString();
    	if(vmri.getPowerState() == VirtualMachinePowerState.poweredOn)
    		res=true;
    	else res=false;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	
    return res;
    	
    }
	///// ALARM MANAGER CODE ///
	public static void setalarm(VirtualMachine vm, ServiceInstance si) throws Exception
	{
		System.out.println("setting alarm for "+ vm.getName());
		  AlarmManager alarmMgr = si.getAlarmManager();
		      System.out.println("setting alarm. Please wait.");
		    //This will remove alarm if it exists...
		    Alarm alarms[]=alarmMgr.getAlarm(vm);
		    for(int i=0;i<alarms.length;i++)
		    {
		    		alarms[i].removeAlarm();
		    }

		    AlarmSpec spec = new AlarmSpec();
		    
		    StateAlarmExpression expression = createStateAlarmExpression();
		    AlarmAction emailAction = createAlarmTriggerAction(createEmailAction());
		    AlarmAction methodAction = createAlarmTriggerAction(createPowerOnAction());
		    GroupAlarmAction gaa = new GroupAlarmAction();

		    gaa.setAction(new AlarmAction[]{emailAction, methodAction});
		    spec.setAction(gaa);
		    spec.setExpression(expression);
		    //Date date = new Date();
		    spec.setName(vm.getName().toString()+":"+new Date().toString());
		    spec.setDescription("Monitor VM state and send email " + "and power it on if VM powers off");
		    spec.setEnabled(true);    
		    spec.setAction(emailAction);
		   
		  		    
		    AlarmSetting as = new AlarmSetting();
		    as.setReportingFrequency(0); //as often as possible
		    as.setToleranceRange(0);
		    
		    spec.setSetting(as);
		    try{
		    alarmMgr.createAlarm(vm, spec);
		    }catch (Exception e)
		    {
		    	
		    	System.out.println("Duplicate alarm name error for: "+vm.getName());
		    	System.out.println(e.getClass().getName());
		    	//e.printStackTrace();
		    	if(e instanceof com.vmware.vim25.DuplicateName )
		    	{
		    		System.out.println("Alarm already exists for: "+vm.getName());
		    		
		    	}
		    }
		  }

		 public static StateAlarmExpression createStateAlarmExpression()
		  {
		    StateAlarmExpression expression = 
		      new StateAlarmExpression();
		    expression.setType("VirtualMachine");
		    expression.setStatePath("runtime.powerState");
		    expression.setOperator(StateAlarmOperator.isEqual);
		    expression.setRed("poweredOff");
		    return expression;
		  }

		  public static MethodAction createPowerOnAction() 
		  {
		    MethodAction action = new MethodAction();
		    action.setName("PowerOffVM_Task");
		    MethodActionArgument argument = new MethodActionArgument();
		    argument.setValue(null);
		    action.setArgument(new MethodActionArgument[] { argument });
		    return action;
		  }
		  
		  public static SendEmailAction createEmailAction() 
		  {
		    SendEmailAction action = new SendEmailAction();
		    action.setToList("sagarruchandani@gmail.com");
		    action.setCcList("utsav2601@gmail.com");
		    action.setSubject("Alarm - {alarmName} on {targetName}\n");
		    action.setBody("Description:{eventDescription}\n"
		        + "TriggeringSummary:{triggeringSummary}\n"
		        + "newStatus:{newStatus}\n"
		        + "oldStatus:{oldStatus}\n"
		        + "target:{target}");
		    return action;
		  }

		  public static AlarmTriggeringAction createAlarmTriggerAction(
		      Action action) 
		  {
		    AlarmTriggeringAction alarmAction = 
		      new AlarmTriggeringAction();
		    alarmAction.setYellow2red(true);
		    alarmAction.setAction(action);
		    return alarmAction;
		  }
	
	
	////////////////////// GET ALARM STATUS ////
	public static boolean getalarm(ServiceInstance si,VirtualMachine vm)
    {
    	boolean res=false;
    	AlarmManager a = si.getAlarmManager();

    	try
    	{
    		
    		Alarm [] alarms= a.getAlarm(vm);
    		
    		if(alarms.length>0)
    		{
    			
    			boolean al= false;
    			for(int i=0;i<alarms.length;i++)
    			{
    				String name=alarms[i].getAlarmInfo().getName();
    				if(name.equalsIgnoreCase("PoweredOff"))
    				{
    					System.out.println("Required Alarm Found");
    					
    					al=true;
    					res=true;
    					
    					return res;
    				}
      			}
    			
    			if(al==false)
    			{
    				//set alarm
    				System.out.println("no alarms match the required one.. setting new alarm..");
    				return res=false;
    				//setalarm();
    			}
    			
    		}
    		else
    		{
    			
    			System.out.println("No alarms set. Setting new alarm");
    			return res=false;
    			
    		}
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.toString());
    	}
  
    	return res;
    }
    
	public static void MigrateVM(ServiceInstance si,VirtualMachine vmName,Folder rootFolder,HostSystem newhost) throws Exception
	{
		
		try{
		   
			System.out.println("MIGRATING TO NEW HOST: "+newhost.getName());
		    
		    ComputeResource cr = (ComputeResource) newhost.getParent();
		  
		    Task task = vmName.migrateVM_Task(cr.getResourcePool(), newhost,
		        VirtualMachineMovePriority.highPriority, 
		        VirtualMachinePowerState.poweredOff);
		  
		    
		   
		    
		    
		    if(task.waitForMe()==Task.SUCCESS)
		    {
		      System.out.println("Cold Migration successful!");
		    }
		    else
		    {
		      System.out.println("Something went wrong!");
		      TaskInfo info = task.getTaskInfo();
		      System.out.println(info.getError().getFault());
		    }
		} catch ( Exception e ) 
        { 
			 VirtualMachineRuntimeInfo vmri=vmName.getRuntime();
		    	//String state=vmri.getPowerState().toString();
		    	System.out.println("VM POWER STATE IS: "+vmri.getPowerState());
			
			System.out.println( e.toString() ) ; }
	}
	
	public static boolean revertToSnapshot(VirtualMachine vm)
    {
    	System.out.println("reverting VM "+vm.getName());
    	try
    	{
    		
    		Task t1 = vm.getCurrentSnapShot().revertToSnapshot_Task(null);
    		
    		System.out.println("reverting VM "+vm.getName());
    		vm.getCurrentSnapShot().toString();
    		if(t1.waitForTask()==Task.SUCCESS)
    			return true;
    		else return false;
    	
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.toString());
    	}
    	return false;
    	
    }
	
	///// ADD NEW HOST TO VCENTER
	public static boolean addNewHost(Datacenter dc,HostConnectSpec hs)
	{
	    	boolean ret = false;
	    	try 
			{	
				 ComputeResourceConfigSpec crcs = new ComputeResourceConfigSpec();
				 Task t = dc.getHostFolder().addStandaloneHost_Task(hs,crcs, true);
				 if(t.waitForTask() == t.SUCCESS)
				 {
					 ret = true;
				 }
				 else
				 {
					 ret = false;
				 }
			}   
			catch (Exception e)
			{
				System.out.println(e.toString());
				System.out.println("Unable to connect to Vsphere server");
			}
	    	return ret;
		}
	
	public static boolean revertHostSnapshot(ManagedEntity[] mes,HostSystem host)
    {	boolean result=false;

    	try
    	{
   
    		String str = host.getName().toString();
    		String str1 = str.substring(7);
 
    		for (int i =0;i < mes.length; i ++)
    		{
    		VirtualMachine vm= (VirtualMachine)mes[i];
    	
    			String vmName = vm.getName();
    			if (vmName.contains(str1))
    			{
    				Task t1 = vm.getCurrentSnapShot().revertToSnapshot_Task(null);
    	    		vm.getCurrentSnapShot().toString();
    				System.out.println("Reverting Host Snapshot for: "+vm.getName());
    				if(t1.waitForTask()==t1.SUCCESS)
    	    			result=true;
    	    		else
    	    			result=false;
    				
    			}
    		}
    		
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.toString());
    		result=false;
    		return result;
    	}
    	return result;
    }
    
    
    public static boolean takeHostSnapshot(ManagedEntity[] mes,HostSystem host)
    {
    	try
    	{
    		
    		String str = host.getName().toString();
    		String str1 = str.substring(7);
    		for (int i =0;i < mes.length; i ++)
    		{
    		VirtualMachine vm= (VirtualMachine)mes[i];
    		
    			String vmName = vm.getName();
    			if (vmName.contains(str1))
    			{
    				
    				vm.removeAllSnapshots_Task();
    				vm.createSnapshot_Task("Snapshot Name: "+vm.getName(),"Snapshot for " + vm.getName(), false, false);
    				System.out.println("Host Snapshot taken for: "+vm.getName());
    				return true;
    					
    			}
    		}
    		System.out.println("Sorry could not take snapshot for host: "+host.getName());
    		return false;
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.toString());
    	}
		return false;
    }
	
	
}