package cm.antic.cell_geolocator.controller;

import cm.antic.cell_geolocator.service.PriorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private PriorityService priorityService;

    @GetMapping("/priorities")
    public List<String> getPriorities() {
        return priorityService.getProviderPriorities();
    }

    @PostMapping("/priorities")
    public void setPriorities(@RequestBody List<String> priorities) {
        priorityService.setProviderPriorities(priorities);
    }

}
