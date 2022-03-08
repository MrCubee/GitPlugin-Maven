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

    public static final String PROPERTY_BRANCH_NAME = "git.branch.name";
    public static final String PROPERTY_BRANCH_FULL_NAME = "git.branch.name_full";
    public static final String PROPERTY_BRANCH_AUTHORS = "git.branch.authors";
    public static final String PROPERTY_LAST_COMMIT_SHA1 = "git.commit.last.sha1";
    public static final String PROPERTY_LAST_COMMIT_SHORT_SHA1 = "git.commit.last.sha1_short";
    public static final String PROPERTY_LAST_COMMIT_AUTHOR = "git.commit.last.author";

    @Parameter(property = "project", readonly = false)
    private MavenProject project;

    @Parameter(property = PROPERTY_BRANCH_NAME, readonly = false, defaultValue = "none")
    private String branchName;

    @Parameter(property = PROPERTY_BRANCH_FULL_NAME, readonly = false, defaultValue = "none")
    private String branchFullName;

    @Parameter(property = PROPERTY_BRANCH_AUTHORS, readonly = false)
    private String branchAuthors;

    @Parameter(property = PROPERTY_LAST_COMMIT_SHA1, readonly = false, defaultValue = "none")
    private String lastCommitSha1;

    @Parameter(property = PROPERTY_LAST_COMMIT_SHORT_SHA1, readonly = false, defaultValue = "none")
    private String lastCommitSha1Short;

    @Parameter(property = PROPERTY_LAST_COMMIT_AUTHOR, readonly = false, defaultValue = "nobody")
    private String lastCommitAuthor;

    private static String personIndentsToString(final Set<String> authorNames) {
        final StringBuilder strBuilder;

        if (authorNames == null || authorNames.isEmpty())
            return "";
        strBuilder = new StringBuilder();
        for (String name : authorNames) {
            if (strBuilder.length() != 0)
                strBuilder.append(", ");
            strBuilder.append(name);
        }
        return strBuilder.toString();
    }

    private void registerBranchProperties(final Properties properties, final Repository repository) throws IOException {
        final RevWalk revWalk;
        final Set<String> authors;
        RevCommit revCommit;
        PersonIdent author;

        if (properties == null || repository == null)
            return;
        revWalk = new RevWalk(repository);
        authors = new HashSet<String>();
        properties.setProperty(PROPERTY_BRANCH_NAME, this.branchName = repository.getBranch());
        properties.setProperty(PROPERTY_BRANCH_FULL_NAME, this.branchFullName = repository.getFullBranch());
        for (Ref ref : repository.getRefDatabase().getRefs()) {
            revCommit = revWalk.parseCommit(ref.getObjectId());
            revWalk.markStart(revCommit);
        }
        for (RevCommit commit : revWalk) {
            author = new CraftPersonIndent(commit.getAuthorIdent());
            getLog().info("Parsing " + commit.getName() + " (author: " + author.toExternalString() + ") ...");
            authors.add(author.getName());
        }
        properties.setProperty(PROPERTY_BRANCH_AUTHORS, this.branchAuthors = personIndentsToString(authors));
    }

    private void registerLastCommitProperties(final Properties properties, final RevCommit revCommit) {
        if (properties == null || revCommit == null)
            return;
        properties.setProperty(PROPERTY_LAST_COMMIT_AUTHOR, this.lastCommitAuthor = revCommit.getAuthorIdent().getName());
        properties.setProperty(PROPERTY_LAST_COMMIT_SHA1, this.lastCommitSha1 = revCommit.getName());
        properties.setProperty(PROPERTY_LAST_COMMIT_SHORT_SHA1, this.lastCommitSha1Short = this.lastCommitSha1.substring(0, 7));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Properties properties;
        final Git git;
        final Repository repository;
        final ObjectId lastCommitId;

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
            registerBranchProperties(properties, repository);
            lastCommitId = repository.resolve(Constants.HEAD);
            if (lastCommitId == null) {
                getLog().error("Commit not found.");
                return;
            }
            registerLastCommitProperties(properties, new RevWalk(repository).parseCommit(lastCommitId));
            getLog().info("Git commits parsed.");
        } catch (IOException exception) {
            getLog().error(exception);
        }
    }
}
