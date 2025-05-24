package org.example.stockmarketsimulator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {
    
@GetMapping(value = "/**/{path:[^\\.]*}")
public String forward() {
    return "forward:/index.html";
}

}
