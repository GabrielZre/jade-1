package jadelab1;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MyAgent extends Agent {

	public record Request(String content, String ontology, String response) {}
	public Map<String, Request> requests = new HashMap<>();

	protected void setup () {
		addBehaviour(new MyCyclicBehaviour(this));
		//doDelete();
	}
	protected void takeDown() {
		displayResponse("See you");
	}
	public void displayResponse(String message) {
		JOptionPane.showMessageDialog(null,message,"Message",JOptionPane.PLAIN_MESSAGE);
	}
	public void displayHtmlResponse(String html) {
		JTextPane tp = new JTextPane();
		JScrollPane js = new JScrollPane();
		js.getViewport().add(tp);
		JFrame jf = new JFrame();
		jf.getContentPane().add(js);
		jf.pack();
		jf.setSize(400,500);
		jf.setVisible(true);
		tp.setContentType("text/html");
		tp.setEditable(false);
		tp.setText(html);
	}
}

class MyCyclicBehaviour extends CyclicBehaviour {
	MyAgent myAgent;
	public MyCyclicBehaviour(MyAgent myAgent) {
		this.myAgent = myAgent;
	}
	public void action() {
		ACLMessage message = myAgent.receive();
		if (message == null) {
			block();
		} else {
			String ontology = message.getOntology();
			String content = message.getContent();
			String msgId = message.getInReplyTo();

			System.out.println("msgId : " + msgId);

			int performative = message.getPerformative();
			if (performative == ACLMessage.REQUEST)
			{
				//I cannot answer but I will search for someone who can
				DFAgentDescription dfad = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setName("dictionary");
				dfad.addServices(sd);
				try
				{
					DFAgentDescription[] result = DFService.search(myAgent, dfad);
					if (result.length == 0) myAgent.displayResponse("No service has been found ...");
					else
					{
						msgId = generateId();

						String foundAgent = result[0].getName().getLocalName();
						String info = "Agent %s is service provider, sending message. [%s]"
								.formatted(foundAgent, msgId);
						myAgent.displayResponse(info);
						ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
						forward.addReceiver(new AID(foundAgent, AID.ISLOCALNAME));
						forward.setContent(content);
						forward.setOntology(ontology);
						forward.setReplyWith(msgId);
						myAgent.send(forward);

						MyAgent.Request request = new MyAgent.Request(content, ontology, null);
						this.myAgent.requests.put(msgId, request);
					}
				}
				catch (FIPAException ex)
				{
					ex.printStackTrace();
					myAgent.displayResponse("Problem occured while searching for a service ...");
				}
			}
			else
			{	//when it is an answer
				MyAgent.Request request = myAgent.requests.get(msgId);
				MyAgent.Request withResponse = new MyAgent.Request(
						request.content(), request.ontology(), content);
				myAgent.requests.put(msgId, withResponse);

				myAgent.displayResponse("%s received".formatted(msgId));
				myAgent.displayHtmlResponse(request.content() + "   " + content);
			}
		}
	}

	protected String generateId() {
		return String.valueOf( new Random().nextInt() );
	}
}
