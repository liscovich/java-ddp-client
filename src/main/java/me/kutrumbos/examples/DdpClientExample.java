package me.kutrumbos.examples;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Observer;

import me.kutrumbos.DdpClient;

/**
 * Simple example of DDP client use-case that just involves 
 * 		making a connection to a locally hosted Meteor server
 * @author peterkutrumbos
 *
 */
public class DdpClientExample {

	public static void main(String[] args) {
		
		// specify location of Meteor server (assumes it is running locally) 
		String meteorIp = "localhost";
		Integer meteorPort = 3000;

		try {
			
			// create DDP client instance
			DdpClient ddp = new DdpClient(meteorIp, meteorPort);
			
			// create DDP client observer
			Observer obs = new SimpleDdpClientObserver();
			
			// add observer
			ddp.addObserver(obs);
						
			// make connection to Meteor server
			ddp.connect();
			
			while(true) {
				try {
					Thread.sleep(5000);

					System.out.println("calling remote method...");
					
					Object[] methodArgs = new Object[1];
					methodArgs[0] = new Date().toString();
					ddp.call("update_time", methodArgs);

				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
			}
					
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}			
	}

	
	
}
