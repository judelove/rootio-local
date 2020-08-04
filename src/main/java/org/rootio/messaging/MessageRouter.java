package org.rootio.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MessageRouter {
    HashMap<String, List<BroadcastReceiver>> registeredReceivers;

    MessageRouter()
    {
        registeredReceivers = new HashMap<>();
    }

    public void register(BroadcastReceiver receiver, String filter)
    {
        if(!registeredReceivers.containsKey(filter))
        {
            registeredReceivers.put(filter, new ArrayList<>());
        }
        if(!registeredReceivers.get(filter).contains(receiver))
        {
            registeredReceivers.get(filter).add(receiver);
        }
    }

    public void specicast(Message message, String filter)
    {
        if(registeredReceivers.containsKey(filter))
        {
            registeredReceivers.get(filter).forEach(broadcastReceiver -> broadcastReceiver.onReceive(message));
        }
    }

    private void broadcast(Message message)
    {
        registeredReceivers.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .forEach(broadcastReceiver -> broadcastReceiver.onReceive(message));

    }

    private void deregister(BroadcastReceiver receiver, String filter)
    {
        if(registeredReceivers.containsKey(filter))
        {
            registeredReceivers.get(filter).remove(receiver);
        }
    }
}
