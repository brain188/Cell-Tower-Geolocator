package cm.antic.cell_geolocator.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PriorityService {

    @Value("${providers.priority:OpenCellID,UnwiredLabs,Mozilla}")
    private String priorityString;

    private List<String> priorities;

    public List<String> getProviderPriorities() {
        if (priorities == null) {
            priorities = Arrays.asList(priorityString.split(","));
        }
        return priorities;
    }

    public void setProviderPriorities(List<String> newPriorities) {
        this.priorities = newPriorities;
        this.priorityString = String.join(",", newPriorities);
    }

}
