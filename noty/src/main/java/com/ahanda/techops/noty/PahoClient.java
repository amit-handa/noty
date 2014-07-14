package com.ahanda.techops.noty;

import java.io.IOException;
import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class PahoClient
{
    static String clientId = "ahanda";

    static String uid = "Ukk44aEMFRD3xQ2l";

    static String token = "KANyn56SSwg=";

    static String uri = "tcp://localhost:8080";
    
    static MqttAsyncClient m = null;

    public static void main(String[] args)
    {
        try
        {
            while (true)
            {
                Scanner in = new Scanner(System.in);
                int inChar = in.nextInt();
                System.out.println("You entered : "+inChar);
                if(inChar == 1)
                    connectAndSubscribe();
                else if(inChar == 2)
                    publish();
                else if(inChar == 3)
                    isConnected();
                else
                    break;
            }
        }
        catch (MqttException e)
        {
            System.out.println(e.getReasonCode());
            e.printStackTrace();
        }
    }

    private static void isConnected()
    {
        boolean isConnected = false;
        if(m != null)
        {
            isConnected = m.isConnected();
        }
    }

    static int count = 0;
    private static void publish() throws MqttPersistenceException, MqttException
    {
        count++;
        String msg = String.format("{\"t\": \"m\",\"to\": \"+919999711370\",\"d\":{\"hm\":\"%s\",\"i\":\"12312\", \"ts\":1234567890}}", count);
        m.publish(uid+"/p",msg.getBytes(),1,false,new IMqttActionListener()
        {
            
            @Override
            public void onSuccess(IMqttToken arg0)
            {
                boolean isCom = arg0.isComplete();
                System.out.println(isCom);
            }
            
            @Override
            public void onFailure(IMqttToken arg0, Throwable arg1)
            {
                System.out.println(arg1.getLocalizedMessage());        
            }
        }, null);
    }

    private static void connectAndSubscribe() throws MqttException
    {
        m = new MqttAsyncClient(uri, clientId, null);
        System.out.println("M is : " + m.toString());
        MqttConnectOptions op = new MqttConnectOptions();
        op.setUserName(uid);
        op.setPassword(token.toCharArray());
        op.setConnectionTimeout(0);
        IMqttToken t = m.connect(op);
        t.setActionCallback(new IMqttActionListener()
        {
            @Override
            public void onSuccess(IMqttToken arg0)
            {
                System.out.println("Yipee connection made successfully");
                System.out.println("Success connect obj : " + arg0.getClient());
                try
                {
                    String [] topics = new String[3];
                    topics[0] = uid+"/s";
                    topics[1] = uid+"/a";
                    topics[2] = uid+"/u";
                    int []qos = new int []{1,1,1};
                    m.subscribe(topics, qos).setActionCallback(new IMqttActionListener()
                    {
                        
                        @Override
                        public void onSuccess(IMqttToken arg0)
                        {
                            System.out.println("Yipee subscribed to all topics");
                            
                        }
                        
                        @Override
                        public void onFailure(IMqttToken arg0, Throwable arg1)
                        {
                            System.out.println("Error subscribing to topics : " + arg1.getMessage());
                        }
                    });
                    
                    /*
                    m.subscribe(uid + "/s", 1).setActionCallback(new IMqttActionListener()
                    {
                        @Override
                        public void onSuccess(IMqttToken arg0)
                        {
                            System.out.println("Yipee subscribed to /s");
                        }

                        @Override
                        public void onFailure(IMqttToken arg0, Throwable arg1)
                        {
                            System.out.println("Error subscribing to /s : " + arg1.getMessage());
                        }
                    });
                    m.subscribe(uid + "/u", 1).setActionCallback(new IMqttActionListener()
                    {
                        @Override
                        public void onSuccess(IMqttToken arg0)
                        {
                            System.out.println("Yipee subscribed to /u");
                        }

                        @Override
                        public void onFailure(IMqttToken arg0, Throwable arg1)
                        {
                            System.out.println("Error subscribing to /u : " + arg1.getMessage());
                        }
                    });
                    m.subscribe(uid + "/a", 1).setActionCallback(new IMqttActionListener()
                    {
                        @Override
                        public void onSuccess(IMqttToken arg0)
                        {
                            System.out.println("Yipee subscribed to /a");
                        }

                        @Override
                        public void onFailure(IMqttToken arg0, Throwable arg1)
                        {
                            System.out.println("Error subscribing to /a : " + arg1.getMessage());
                        }
                    });
                    */
                }
                catch (MqttException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken arg0, Throwable arg1)
            {
                MqttException ex = arg0.getException();
                System.out.println("ReasonCode : "+ex.getReasonCode());
                System.out.println("Connection failed : " + arg1.getMessage());
            }
        });
        m.setCallback(new MqttCallback()
        {

            @Override
            public void messageArrived(String arg0, MqttMessage arg1) throws Exception
            {
                String msg = new String(arg1.getPayload(), "UTF-8");
                System.out.println("Message arrived from mqtt server : " + msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken arg0)
            {
                System.out.println("Message delivered to mqtt server for id : " + arg0.getMessageId());
            }

            @Override
            public void connectionLost(Throwable arg0)
            {
                System.out.println("Connection lost .....");

            }
        });
        
    }
}