package fr.mrcubee.maven.git;

import org.eclipse.jgit.lib.PersonIdent;

import java.util.Objects;

public class CraftPersonIndent extends PersonIdent {

    public CraftPersonIndent(final PersonIdent personIdent) {
        super(personIdent.getName(), personIdent.getEmailAddress(), personIdent.getWhen().getTime(), personIdent.getTimeZoneOffset());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getEmailAddress());
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof PersonIdent && object.hashCode() == hashCode();
    }

    @Override
    public String toExternalString() {
        return String.format("%s <%s>", getName(), getEmailAddress());
    }
}
