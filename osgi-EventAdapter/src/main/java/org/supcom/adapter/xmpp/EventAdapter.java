package org.supcom.adapter.xmpp;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.osgi.framework.BundleContext;

import org.apache.commons.logging.Log;
import org.wso2.carbon.device.mgt.output.adapter.xmpp.util.XMPPEventAdapterConstants;
import org.wso2.carbon.device.mgt.output.adapter.xmpp.util.XMPPAdapterPublisher;
import org.wso2.carbon.event.output.adapter.core.EventAdapterUtil;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapter;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class XMPPEventAdapter implements OutputEventAdapter {
    private OutputEventAdapterConfiguration eventAdapterConfiguration;
    private Map<String, String> globalProperties;
    private XMPPAdapterPublisher xmppAdapterPublisher;
    private static ThreadPoolExecutor threadPoolExecutor;
    private int tenantId;

    protected void activate(BundleContext context) {
    try {
        OutputEventAdapterFactory xmppEventAdapterFactory = new XMPPEventAdapterFactory();
        context.getBundleContext().registerService(OutputEventAdapterFactory.class.getName(),
                                                   xmppEventAdapterFactory, null);
        if (log.isDebugEnabled()) {
            log.debug("The XMPP publisher service has been deployed successfully");
        }
    } catch (RuntimeException e) {
        log.error("Exception occurred when deploying XMPP publisher service", e);
    }
    }

    public XMPPEventAdapter(OutputEventAdapterConfiguration eventAdapterConfiguration,
                            Map<String, String> globalProperties) {
        this.eventAdapterConfiguration = eventAdapterConfiguration;
        this.globalProperties = globalProperties;
    }

    class XMPPSender implements Runnable {
        String jid;
        String subject;
        String message;
        String messageType;
        XMPPSender(String jid, String subject, String message, String messageType) {
            this.jid = jid;
            this.message = message;
            this.subject = subject;
            this.messageType = messageType;
        }
        @Override
        public void connect() {
            try {
					ConnectionConfiguration config = new ConnectionConfiguration("127.0.0.1", 5222);
					config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
					config.setDebuggerEnabled(false);
					//config.setSendPresence(true);
					XMPPConnection con=new XMPPConnection(config);
					con.connect();
					System.out.println("connection");
				}catch(Exception e) {e.printStackTrace();}
        }
    }
   
    
  
    @Override
    public void publish(Object message, Map<String, String> dynamicProperties) {
        String jid = dynamicProperties.get(XMPPEventAdapterConstants.ADAPTER_CONF_JID);
        String subject = dynamicProperties.get(XMPPEventAdapterConstants.ADAPTER_CONF_SUBJECT);
        String messageType = dynamicProperties.get(XMPPEventAdapterConstants.ADAPTER_CONF_MESSAGETYPE);
        try {
            threadPoolExecutor.submit(new XMPPSender(jid, subject, (String)message, messageType));
        } catch (RejectedExecutionException e) {
            EventAdapterUtil.logAndDrop(eventAdapterConfiguration.getName(), message, "Job queue is full", e, log,
                                        tenantId);
        }
    }
    @Override
    public void disconnect() {
        try {
            if (xmppAdapterPublisher != null) {
                xmppAdapterPublisher.close();
                xmppAdapterPublisher = null;
            }
        } catch (OutputEventAdapterException e) {
            log.error("Exception when closing the xmpp publisher connection on Output XMPP Adapter '" +
                              eventAdapterConfiguration.getName() + "'", e);
        }
    }
    
    
}
