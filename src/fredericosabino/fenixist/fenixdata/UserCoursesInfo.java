package fredericosabino.fenixist.fenixdata;

import java.io.Serializable;

public class UserCoursesInfo implements Serializable {
		/**
	 * 
	 */
	private static final long serialVersionUID = 7068559614760351772L;
		private Enrolment[] enrolments;
		private Teaching[] teaching;
		public Enrolment[] getEnrolments() {
			return enrolments;
		}
		public Teaching[] getTeaching() {
			return teaching;
		}
		
		/*Represents each course taking*/
		public class Enrolment implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = -9007687956996710952L;
			private long id;
			private String acronym;
			private String name;
			private String academicTerm;
			private String url;
			private String grade;
			public long getId() {
				return id;
			}
			public String getAcronym() {
				return acronym;
			}
			public String getName() {
				return name;
			}
			public String getAcademicTerm() {
				return academicTerm;
			}
			public String getUrl() {
				return url;
			}
			public String getGrade() {
				return grade;
			}
			
		}
		
		/*Represents each course teaching*/
		public class Teaching implements Serializable {
			private static final long serialVersionUID = -3217433527270528518L;
			private long id;
			private String acronym;
			private String name;
			private String academicTerm;
			private String url;
			public long getId() {
				return id;
			}
			public String getAcronym() {
				return acronym;
			}
			public String getName() {
				return name;
			}
			public String getAcademicTerm() {
				return academicTerm;
			}
			public String getUrl() {
				return url;
			}
			
		}
}
