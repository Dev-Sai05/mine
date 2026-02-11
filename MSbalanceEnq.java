
package com.example.sysc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MSbalanceEnq {

    @Autowired
    private SyscDcnService syscDcnService;

    public void processTransaction() {

        String mode = syscDcnService.getCurrentMode();

        if ("DAY".equals(mode)) {
            System.out.println("Using DAY DB");
        } else if ("NIGHT_REF".equals(mode)) {
            System.out.println("Using NIGHT REF DB");
        } else {
            System.out.println("Using NIGHT DAY DB");
        }
    }
}
