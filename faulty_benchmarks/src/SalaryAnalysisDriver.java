import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

import java.io.IOException;
@RunWith(JQF.class)

public class SalaryAnalysisDriver {

@Fuzz
    public void testSalaryAnalysis(String fileName) throws IOException {
        System.out.println("edu.ucla.cs.bigfuzz.customarray.inapplicable.OutofJDU.OutofJDUDriver::testOutofJDU: "+fileName);
        SalaryAnalysis analysis = new SalaryAnalysis();
        analysis.OutofJDU(fileName);
    }
}