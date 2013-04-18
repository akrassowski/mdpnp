/*******************************************************************************
 * Copyright (c) 2012 MD PnP Program.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package org.mdpnp.devices.nellcor.pulseox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mdpnp.comms.Gateway;
import org.mdpnp.comms.data.numeric.MutableNumericUpdate;
import org.mdpnp.comms.data.numeric.MutableNumericUpdateImpl;
import org.mdpnp.comms.nomenclature.PulseOximeter;
import org.mdpnp.comms.serial.AbstractSerialDevice;
import org.mdpnp.comms.serial.SerialProvider;
import org.mdpnp.comms.serial.SerialSocket.DataBits;
import org.mdpnp.comms.serial.SerialSocket.Parity;
import org.mdpnp.comms.serial.SerialSocket.StopBits;

public class DemoN595 extends AbstractSerialDevice {
	private final MutableNumericUpdate pulseUpdate = new MutableNumericUpdateImpl(PulseOximeter.PULSE);
	private final MutableNumericUpdate spo2Update = new MutableNumericUpdateImpl(PulseOximeter.SPO2);
	private final MutableNumericUpdate pulseUpperUpdate = new MutableNumericUpdateImpl(PulseOximeter.PULSE_UPPER);
	private final MutableNumericUpdate pulseLowerUpdate = new MutableNumericUpdateImpl(PulseOximeter.PULSE_LOWER);
	private final MutableNumericUpdate spo2UpperUpdate = new MutableNumericUpdateImpl(PulseOximeter.SPO2_UPPER);
	private final MutableNumericUpdate spo2LowerUpdate = new MutableNumericUpdateImpl(PulseOximeter.SPO2_LOWER);
	
	private class MyNellcorN595 extends NellcorN595 {
		public MyNellcorN595() throws NoSuchFieldException, SecurityException, IOException {
			super();
		}
		
		@Override
		public void firePulseOximeter() {
			pulseUpdate.set(getHeartRate(), getTimestamp());
			spo2Update.set(getSpO2(), getTimestamp());
			gateway.update(pulseUpdate, spo2Update);
		}
		@Override
		public void fireAlarmPulseOximeter() {
			pulseLowerUpdate.set(getPRLower(), getTimestamp());
			pulseUpperUpdate.set(getPRUpper(), getTimestamp());
			spo2LowerUpdate.set(getSpO2Lower(), getTimestamp());
			spo2UpperUpdate.set(getSpO2Upper(), getTimestamp());
			gateway.update(pulseLowerUpdate, pulseUpperUpdate, spo2LowerUpdate, spo2UpperUpdate);
		}
		@Override
		public void fireDevice() {
			synchronized(this) {
				inited = true;
				this.notifyAll();
			}
			gateway.update(nameUpdate, guidUpdate);
		}
		@Override
		protected void setName(String name) {
			nameUpdate.setValue(name);
		}
		@Override
		protected void setGuid(String guid) {
			guidUpdate.setValue(guid);
		}
	}

	private final MyNellcorN595 fieldDelegate;
	private boolean inited = false;
	
	@Override
	protected void process(InputStream inputStream) throws IOException {
		fieldDelegate.setInputStream(inputStream);
		fieldDelegate.run();
	}
	
	private static final byte[] dumpInstrumentInfo = new byte[] {0x03, 0x03, 0x31, 0x0D, 0x0A, 0x30, 0x0D, 0x0A};
	
	@Override
	protected boolean doInitCommands(OutputStream outputStream) throws IOException {
		inited = false;
		outputStream.write(dumpInstrumentInfo);
		long start = System.currentTimeMillis();
		synchronized(this) {
			while(!inited) {
				try {
					this.wait(500L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if( (System.currentTimeMillis()-start) >= getMaximumQuietTime()) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	public DemoN595(Gateway gateway) throws NoSuchFieldException, SecurityException, IOException {
		super(gateway);
		add(pulseUpdate, spo2Update, pulseLowerUpdate, pulseUpperUpdate, spo2LowerUpdate, spo2UpperUpdate);
		this.fieldDelegate = new MyNellcorN595();
		nameUpdate.setValue(NellcorN595.ROOT_NAME);
	}

	@Override
	protected long getMaximumQuietTime() {
		return 3000L;
	}
	
	@Override
	public SerialProvider getSerialProvider() {
		SerialProvider serialProvider =  super.getSerialProvider();
		serialProvider.setDefaultSerialSettings(9600, DataBits.Eight, Parity.None, StopBits.One);
		return serialProvider;
	}
	@Override
	protected String iconResourceName() {
		return "n595.png";
	}
}