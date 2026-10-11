package es.rodrigonant.p2ai.xls2llm.aop;

public class Logger {
	
	private String className;

	public Logger(Class<?> class1) {
		this.className = class1.getName();
	}

	public void error(String msg) {
		this.log("ERROR", msg);
	}
	
	public void info(String msg) {
		this.log("INFO", msg);
	}
	
	public void debug(String msg) {
//		this.log("DEBUG", msg);
	}
	
	public void trace(String msg) {
//		this.log("TRACE", msg);
	}
	
	
	private void log(String severity, String msg) {
		System.out.println(severity +" "+ this.className +": "+ msg);
	}
	
}
