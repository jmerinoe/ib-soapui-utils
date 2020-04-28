import com.eviware.soapui.impl.wsdl.teststeps.RestRequestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestRequestStepResult;
import com.eviware.soapui.model.iface.MessageExchange;
import java.text.SimpleDateFormat;
import java.util.Date;

class LogUtils {

  //Global objects
  def log
  def context
  def testRunner

  def LogUtils(log, context, testRunner) {
      this.log = log
      this.context = context
      this.testRunner = testRunner
  }

  def printResult(){
  	log.info "Ejecutado test " + testRunner.testCase.getName() + " con resultado: " + testRunner.getStatus()
  }

  def printLog(){
  	def stepNumber = 1
  	testRunner.getResults().each{
  		log.info "  > Step " + stepNumber + ": " + it.getTestStep().getName()
  		log.info "  > Type: " + it.getTestStep().config.type
  		log.info "  > Executed: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(it.getTimeStamp()))
  		log.info "  > Duration: " + it.getTimeTaken() + " ms"
  		log.info "  > Status: " + it.getStatus().toString()
  		if (it instanceof WsdlTestRequestStepResult) {
  			log.info "  > Request: " + ((MessageExchange) it).getRequestContent()
  			log.info "  > Response: " + ((MessageExchange) it).getResponseContent()
  		} else if (it instanceof RestRequestStepResult){
  			try {
               	log.info "  > Request: " + new String(((RestRequestStepResult) it).getRawRequestData())
                    log.info "  > Response: " + new String(((RestRequestStepResult) it).getRawResponseData())
                } catch (e) {
               	log.info "  > Request: " + ((RestRequestStepResult) it).getRequestContent()
  				log.info "  > Response: " + ((RestRequestStepResult) it).getResponseContent()
                }
  		}
  		log.info "\n\n"
  		stepNumber++
  	}
  }
  
  def buildLog(){
  	def builder = new StringBuilder()
  	def stepNumber = 1
  	def testResult = "[TEST PASSED]"
  	def testDuration = 0
  	testRunner.getResults().each{
  		builder.append("Step " + stepNumber + ": " + it.getTestStep().getName() + "\n")
  		builder.append("  > Type: " + it.getTestStep().config.type + "\n")
  		builder.append("  > Executed: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(it.getTimeStamp())) + "\n")
  		def duration = it.getTimeTaken()
  		testDuration += duration
  		builder.append("  > Duration: " + duration + " ms" + "\n")
  		def status = it.getStatus().toString()
  		testResult = status.equals("FAILED") ? "[TEST FAILED]" : testResult
  		builder.append("  > Status: " + status + "\n")
  		if (it instanceof WsdlTestRequestStepResult) {
  			builder.append("  > Request: " + ((MessageExchange) it).getRequestContent() + "\n")
  			builder.append("  > Response: " + ((MessageExchange) it).getResponseContent() + "\n")
  		} else if (it instanceof RestRequestStepResult){
  			try {
               	builder.append("  > Request: " + new String(((RestRequestStepResult) it).getRawRequestData()) + "\n")
                    builder.append("  > Response: " + new String(((RestRequestStepResult) it).getRawResponseData()) + "\n")
                } catch (e) {
               	builder.append("  > Request: " + ((RestRequestStepResult) it).getRequestContent() + "\n")
  				builder.append("  > Response: " + ((RestRequestStepResult) it).getResponseContent() + "\n")
                }
  		}
  		builder.append("\n\n")
  		stepNumber++
  	}
  	builder.append("\n\n")
  	builder.append("Test duration: " + testDuration + " ms\n")
  	builder.append(testResult)
  	return builder.toString()
  }
  
}

LogUtils initObj = context.getProperty("LogUtils")
if (initObj == null) {
    initObj = new LogUtils(log, context, context.getTestRunner())
    context.setProperty(initObj.getClass().getName(), initObj)
}