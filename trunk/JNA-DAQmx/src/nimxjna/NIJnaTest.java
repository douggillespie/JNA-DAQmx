package nimxjna;

import com.sun.jna.Platform;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;

import nimxjna.NILibJna.NiJNA.DAQmxDoneEventCallbackPtr;
import nimxjna.NILibJna.NiJNA.DAQmxEveryNSamplesEventCallbackPtr;

public class NIJnaTest {

	NILibJna niJna;
	
	private int nChan = 2;

	private boolean continuous = true;

	public static void main(String[] args) {
		NIJnaTest nijt = new NIJnaTest();
		nijt.run();
	}

	private void run() {
		System.out.printf("Running %d bit platform\n\n", Platform.is64Bit() ? 64 : 32);
		try {
			niJna = new NILibJna();
		} catch (NIJnaException e) {
			System.out.println(e.getMessage());
			return;
		}
		long loadT = niJna.getLoadTime();
		System.out.printf("Ni library took %3.1fms to load\n", loadT/1.0e6);

		int[] serialNum = new int[1];
		int[] isSim = new int[1];
		String[] allNames = niJna.getDeviceList();
		for (int i = 0; i < allNames.length; i++) {
			String devName = allNames[i];
			//		char[] nameCh = devName.toCharArray();
			//		devName.t
			int ans = niJna.ni.DAQmxGetDevSerialNum(devName, serialNum);
			int ans2 = niJna.ni.DAQmxGetDevIsSimulated(devName, isSim);
			System.out.printf("Device %d: %s serial number ans = %d, number = %d (0x%X), simulated: %s\n", 
					i, devName, ans, serialNum[0],serialNum[0],isSim[0]>0?"TRUE":"FALSE");
			byte[] chanData = new byte[1024];
			ans = niJna.ni.DAQmxGetDevAIPhysicalChans(devName, chanData, chanData.length);
			System.out.println(new String(chanData));
			ans = niJna.ni.DAQmxGetDevAOPhysicalChans(devName, chanData, chanData.length);
			System.out.println(new String(chanData));
			niJna.getDeviceAORanges(devName);
		}

		//		% now try to read an analog voltage from the second device. 
		String devName = allNames[1];
		double[][] aiRanges = niJna.getDeviceAIRanges(devName);
		String[] channelNames = niJna.getAIChannelNames(devName);
		LongByReference taskHandle = new LongByReference();
		int iRange = 5;
		int ans = niJna.ni.DAQmxCreateTask("Test1", taskHandle);
		int handle = (int) taskHandle.getValue();
		System.out.printf("Task handle is %d (0x%016X)\n", handle, handle);
		for (int i = 0; i < nChan; i++) {
			errWrap(ans = niJna.ni.DAQmxCreateAIVoltageChan(handle, channelNames[i], "ch"+i, 
					NIConstants.DAQmx_Val_RSE, aiRanges[iRange][0], aiRanges[iRange][1], NIConstants.DAQmx_Val_Volts, null));
		}
		//		double[] volts = new double[1];
		/*
		 * Read single values. 
		 */
		if (continuous == false) {
			DoubleByReference volts = new DoubleByReference();
			ans = niJna.ni.DAQmxStartTask(handle);
			for (int i = 0; i < 10; i++) {
				errWrap(ans = niJna.ni.DAQmxReadAnalogScalarF64(handle, 2, volts, null));
				System.out.printf("Read voltage %d, resp %d value %3.3fV\n", i, ans, volts.getValue());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else {
			/*
			 * Read continuously and extract data from a callback buffer. 
			 */
			int sampleRate = 500000;
			errWrap(niJna.ni.DAQmxCfgSampClkTiming(handle, "", sampleRate, NIConstants.DAQmx_Val_Rising, 
					NIConstants.DAQmx_Val_ContSamps, 0));

			byte[] cd = new byte[0];

			DAQmxEveryNSamplesEventCallback callback = new DAQmxEveryNSamplesEventCallback();
			DoneCallback doneCallback = new DoneCallback();

			errWrap(niJna.ni.DAQmxRegisterEveryNSamplesEvent(handle, NIConstants.DAQmx_Val_Acquired_Into_Buffer, 
					sampleRate/10, 0, callback, null));
			niJna.ni.DAQmxRegisterDoneEvent(handle, 0, doneCallback, null);

			niJna.ni.DAQmxStartTask(handle);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			niJna.ni.DAQmxStopTask(handle);
			niJna.ni.DAQmxClearTask(handle);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private class DAQmxEveryNSamplesEventCallback implements DAQmxEveryNSamplesEventCallbackPtr {

		private int calls = 0;
		@Override
		public int callback(int taskHandle, int everyNsamplesEventType, int nSamples, IntByReference callbackData) {
			double[] readBuff = new double[nSamples*nChan];
			IntByReference sampsRead = new IntByReference();
			IntByReference resd = null;
			int ans;
			int[] sr = new int[1];

			errWrap(ans = niJna.ni.DAQmxReadAnalogF64(taskHandle, nSamples, 0.f, NIConstants.DAQmx_Val_GroupByScanNumber, readBuff, nSamples*nChan, sampsRead, null));
			System.out.printf("Callback %d, ev %d samples %d, read %d, first %5.2f\n", ++calls, everyNsamplesEventType, nSamples, sampsRead.getValue(),
					readBuff[0]);
			return 0;
		}

	}

	private class DoneCallback implements DAQmxDoneEventCallbackPtr {

		@Override
		public int callback(int taskHandle, int status, IntByReference callbackData) {
			System.out.printf("Done callback status %d", status);
			return 0;
		}

	}

	private int errWrap(int err) {
		if (err == 0) {
			return err; 
		}
		String errStr = niJna.getErrorString(err);
		if (errStr != null) {
			System.err.println(errStr);
		}
		return err;
	}

}
