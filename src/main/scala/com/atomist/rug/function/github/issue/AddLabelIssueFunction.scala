package com.atomist.rug.function.github.issue

import com.atomist.rug.function.github.GitHubFunction
import com.atomist.rug.function.github.issue.GitHubIssues.mapIssue
import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.annotation.{Parameter, RugFunction, Secret, Tag}
import com.atomist.rug.spi.{AnnotatedRugFunction, FunctionResponse, JsonBodyOption, StringBodyOption}
import com.typesafe.scalalogging.LazyLogging
import org.kohsuke.github.GitHub

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Label an issue with a known label.
  */
class AddLabelIssueFunction extends AnnotatedRugFunction
  with LazyLogging
  with GitHubFunction {

  @RugFunction(name = "add-label-github-issue", description = "Adds a label to an already existing issue",
    tags = Array(new Tag(name = "github"), new Tag(name = "issues")))
  def invoke(@Parameter(name = "issue") number: Int,
             @Parameter(name = "repo") repo: String,
             @Parameter(name = "owner") owner: String,
             @Parameter(name = "label") label: String,
             @Secret(name = "user_token", path = "github://user_token?scopes=repo") token: String): FunctionResponse = {

    logger.info(s"Invoking addLabelIssue with number '$number', label '$label', owner '$owner', repo '$repo' and token '${safeToken(token)}'")

    Try {
      val gitHub = GitHub.connectUsingOAuth(token)
      val repository = gitHub.getOrganization(owner).getRepository(repo)
      val issue = repository.getIssue(number)
      val labels = issue.getLabels.asScala.map(_.getName).toSeq :+ label
      issue.setLabels(labels: _*)
      mapIssue(repository.getIssue(number))
    } match {
      case Success(response) => FunctionResponse(Status.Success, Some(s"Successfully labelled issue `#$number` in `$owner/$repo`"), None, JsonBodyOption(response))
      case Failure(e) =>
        val msg = s"Failed to label issue `#$number` in `$owner/$repo`"
        logger.error(msg,e)
        FunctionResponse(Status.Failure, Some(msg), None, StringBodyOption(e.getMessage))
    }
  }
}
