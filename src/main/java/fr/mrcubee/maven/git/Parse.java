package fr.mrcubee.maven.git;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Mojo(name = "parse", threadSafe = true, requiresProject = true, defaultPhase = LifecyclePhase.INITIALIZE)
public class Parse extends AbstractMojo {

    @Parameter(property = "project", readonly = false)
    private MavenProject project;

    private static String personIndentsToString(final Set<PersonIdent> personIdents) {
        final StringBuilder strBuilder;

        if (personIdents == null || personIdents.isEmpty())
            return "nobody";
        strBuilder = new StringBuilder();
        for (PersonIdent personIdent : personIdents) {
            if (strBuilder.length() != 0)
                strBuilder.append(", ");
            strBuilder.append(personIdent.getName());
        }
        return strBuilder.toString();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Properties properties;
        final Git git;
        final Repository repository;
        final RevWalk revWalk;
        final ObjectId lastCommitId;
        final String lastCommitSha1;
        final Set<PersonIdent> authors;
        PersonIdent author;
        RevCommit revCommit;

        if (project == null) {
            getLog().error("Maven project is null.");
            return;
        }
        getLog().info("Parsing git repository...");
        properties = project.getProperties();
        try {
            git = Git.open(project.getBasedir());
            repository = git.getRepository();
            if (repository == null) {
                getLog().error("Git repository not found.");
                return;
            }
            revWalk = new RevWalk(repository);
            authors = new HashSet<PersonIdent>();
            properties.setProperty("git.branch.name", repository.getBranch());
            properties.setProperty("git.branch.name_full", repository.getFullBranch());
            for (Ref ref : repository.getRefDatabase().getRefs()) {
                revCommit = revWalk.parseCommit(ref.getObjectId());
                revWalk.markStart(revCommit);
            }
            for (RevCommit commit : revWalk) {
                author = new CraftPersonIndent(commit.getAuthorIdent());
                getLog().info("Parsing " + commit.getName() + " (author: " + author.toExternalString() + ") ...");
                authors.add(author);
            }
            properties.setProperty("git.branch.authors", personIndentsToString(authors));
            lastCommitId = repository.resolve(Constants.HEAD);
            if (lastCommitId == null) {
                properties.setProperty("git.commit.last.sha1", "none");
                properties.setProperty("git.commit.last.sha1_short", "none");
                properties.setProperty("git.commit.last.author", "nobody");
                getLog().error("Commit not found.");
                return;
            }
            revCommit = revWalk.parseCommit(lastCommitId);
            properties.setProperty("git.commit.last.author", revCommit.getAuthorIdent().getName());
            lastCommitSha1 = lastCommitId.getName();
            properties.setProperty("git.commit.last.sha1", lastCommitSha1);
            properties.setProperty("git.commit.last.sha1_short", lastCommitSha1.substring(0, 7));
        } catch (IOException exception) {
            getLog().error(exception);
        }
        getLog().info("Git commits parsed.");
    }
}
