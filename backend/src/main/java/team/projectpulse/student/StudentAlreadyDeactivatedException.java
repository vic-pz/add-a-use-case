package team.projectpulse.student;

public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("Already deactivated.");
    }

}
