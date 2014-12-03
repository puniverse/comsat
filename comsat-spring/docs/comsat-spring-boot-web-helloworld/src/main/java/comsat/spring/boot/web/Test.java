package comsat.spring.boot.web;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.springframework.boot.autoconfigure.web.FiberWebMvcAutoConfiguration;
import co.paralleluniverse.strands.Strand;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Import;

import org.springframework.web.bind.annotation.*;

@RestController
// This will enable fiber-blocking
@Import(FiberWebMvcAutoConfiguration.class)
@EnableAutoConfiguration
public class Test {

    @RequestMapping("/")
    public String home() throws SuspendExecution, InterruptedException {
        System.out.println(new java.util.Date() + ": Strand.isCurrentFiber() = " + Strand.isCurrentFiber());
        System.out.println(new java.util.Date() + ": Strand.currentStrand() instanceof Fiber = " + (Strand.currentStrand() instanceof Fiber));

        System.out.println(new java.util.Date() + ": Before Strand.sleep(1000)");

        Strand.sleep(1000);

        System.out.println(new java.util.Date() + ": After Strand.sleep(1000)");

//        System.out.println(new java.util.Date() + ": About to Fiber.park()");

//        Fiber.park();

//        System.out.println(new java.util.Date() + ": unblocked by someone, Fiber.park() returned");

        System.out.println(new java.util.Date() + ": Returning 'Hello World!'");

        return "Hello World!";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Test.class, args);
    }
}
