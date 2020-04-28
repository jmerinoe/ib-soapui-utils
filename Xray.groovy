import groovy.json.JsonBuilder;

class Xray{
	def log
	def context
	def testRunner

	def Xray(log, context, testRunner) {
		this.log = log
		this.context = context
		this.testRunner = testRunner
	}
	
	def decode(cadena){
		byte[] decoded = cadena.decodeBase64()
		return new String(decoded)
	}
	
	def clientId = decode("RTUxQzlFRTQ3QjNBNDIxNkE0MzY2RDE1M0I2MDE4MjI=")
	def clientSecret = decode("NDhhNmE4MTcwNmJlNGZmOTg4NmJmODA1ODY4YzI0NzU5YzcyMTcxZDU3OWE3ZDdmNDNhMWYyNTI2YTY5ODBjZA==")
	def baseUrl = "https://xray.cloud.xpand-it.com/api/v1"
	def authUrlSufix = "/authenticate"
	def importerUrlSufix = "/import/execution"
	def usuario = "dothraki_Team"
	def testPlanId = context.testCase.testSuite.project.getPropertyValue("jiraXrayTestPlanId")
	def testExecutionId = context.testCase.testSuite.project.getPropertyValue("jiraXrayTestExecutionId")
	
	def getTestDuration(){
		def millis = 0
		testRunner.getResults().each{
			millis += it.getTimeTaken()
		}

		return millis
	}

	def encode(content){
	    return content.bytes.encodeBase64().toString()
	}

	def updateTestResult(){
		def logUtils = context.getProperty("scriptLibrary").get("LogUtils");
		def netClient = context.getProperty("scriptLibrary").get("NetClient");
		def emptyMap = [:]
		
		def initError = false
		def testId
		def fechaInicio
		def fechaFin
		def estado
		def comentarios
		def evidenceName
		def evidenceContent
		try{
			testId = context.testCase.getName().substring(0, context.testCase.getName().indexOf(" ")).trim()
			fechaFin = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", TimeZone.getTimeZone("GMT+1"))
			fechaInicio = new Date(new Date().getTime() - getTestDuration()).format("yyyy-MM-dd'T'HH:mm:ssXXX", TimeZone.getTimeZone("GMT+1"))
			assert testPlanId != null
			assert testExecutionId != null
			estado = testRunner.getStatus().toString() == "FAILED" ? "FAILED" : "PASSED"
			comentarios = context.testCase.getPropertyValue("executionNotes")
			comentarios = comentarios == null ? "" : comentarios
			evidenceName = new Date().format("yyyyMMddHHmmss") + "_" + context.testCase.getName() + ".log"
			evidenceContent = encode(logUtils.buildLog())
		} catch (e){
			log.info "Error intentando actualizar resultado test " + context.testCase.getName() + " en JIRA"
			initError = true
		}
		
		if (!initError){
			def authBody = new JsonBuilder()
			authBody{
				client_id "${clientId}"
				client_secret "${clientSecret}"
			}
	
			def authToken = netClient.executePOSTwJson(baseUrl + authUrlSufix, emptyMap, emptyMap, authBody.toString()).getResponseBody().replace("\"", "")
			def headers = [:]
			headers.put("Authorization", "Bearer " + authToken)
		
			def json = new JsonBuilder()
			json{
				testExecutionKey "${testExecutionId}"
				info{
					user "${usuario}"
					testPlanKey "${testPlanId}"
				}
				tests(
					[
						{
							testKey ("${testId}")
							start ("${fechaInicio}")
							finish ("${fechaFin}")
							comment ("${comentarios}")
							status ("${estado}")
							evidences(
									[
										{
											data ("${evidenceContent}")
											filename ("${evidenceName}")
										}
								
									]
							)
						}
					]
				)
			}

			log.info "Inicio subida resultado y evidencias test " + context.testCase.getName()
			def responseCode = netClient.executePOSTwJson(baseUrl + importerUrlSufix, emptyMap, headers, json.toString()).getResponseCode()
			log.info "Fin subida resultado y evidencias test " + context.testCase.getName()
			def texto = responseCode != null && responseCode >= 200 && responseCode <= 299 ? "Actualizado correctamente en JIRA XRAY el test " + context.testCase.getName() : "Error al intentar actualizar en JIRA XRAY el test " + context.testCase.getName() + " (codigo respuesta " + responseCode + ")"
			log.info texto
		}
	}
}

Xray initObj = context.getProperty("Xray")
if (initObj == null) {
    initObj = new Xray(log, context, context.getTestRunner())
    context.setProperty(initObj.getClass().getName(), initObj)
}