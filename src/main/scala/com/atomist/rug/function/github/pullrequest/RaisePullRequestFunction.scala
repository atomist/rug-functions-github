package com.atomist.rug.function.github.pullrequest

import com.atomist.rug.function.github.{ErrorMessage, GitHubFunction}
import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.annotation.{Parameter, RugFunction, Secret, Tag}
import com.atomist.rug.spi.{AnnotatedRugFunction, FunctionResponse, StringBodyOption}
import com.atomist.source.git.GitHubServices
import com.atomist.source.git.domain.PullRequestRequest
import com.typesafe.scalalogging.LazyLogging

/**
  * Raise a pull request.
  */
class RaisePullRequestFunction
  extends AnnotatedRugFunction
    with LazyLogging
    with GitHubFunction {

  @RugFunction(name = "raise-github-pull-request", description = "Raise a pull request",
    tags = Array(new Tag(name = "github"), new Tag(name = "issues"), new Tag(name = "pr")))
  def invoke(@Parameter(name = "title") title: String,
             @Parameter(name = "body", required = false) body: String,
             @Parameter(name = "base") base: String,
             @Parameter(name = "head") head: String,
             @Parameter(name = "repo") repo: String,
             @Parameter(name = "owner") owner: String,
             @Parameter(name = "apiUrl") apiUrl: String,
             @Secret(name = "user_token", path = "github://user_token?scopes=repo") token: String): FunctionResponse = {

    logger.info(s"Invoking raisePullRequest with title '$title', owner '$owner', repo '$repo', base '$base', head '$head' and token '${safeToken(token)}'")

    try {
      val ghs = GitHubServices(token, apiUrl)
      val prr = PullRequestRequest(title, head, base, body)
      val pr = ghs.createPullRequest(repo, owner, prr)
      FunctionResponse(Status.Success, Some(s"Successfully raised pull request `${pr.number}`"), None)
    } catch {
      case e: Exception =>
        val msg = s"Failed to raise pull request `$title`"
        logger.error(msg, e)
        FunctionResponse(Status.Failure, Some(msg), None, StringBodyOption(ErrorMessage.jsonToString(e.getMessage)))
    }
  }
}
