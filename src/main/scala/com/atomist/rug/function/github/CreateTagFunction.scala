package com.atomist.rug.function.github

import java.time.OffsetDateTime

import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi._
import com.atomist.rug.spi.annotation.{Parameter, RugFunction, Secret, Tag}
import com.fasterxml.jackson.annotation.{JsonFormat, JsonProperty}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

/**
  * Create new tag on a commit.
  */
class CreateTagFunction
  extends AnnotatedRugFunction
    with LazyLogging
    with GitHubFunction {

  import CreateTagFunction._
  import GitHubFunction._

  @RugFunction(name = "create-github-tag", description = "Creates a new tag on a commit",
    tags = Array(new Tag(name = "github"), new Tag(name = "issues")))
  def invoke(@Parameter(name = "tag") tag: String,
             @Parameter(name = "sha") sha: String,
             @Parameter(name = "message") message: String,
             @Parameter(name = "repo") repo: String,
             @Parameter(name = "owner") owner: String,
             @Secret(name = "user_token", path = "github://user_token?scopes=repos") token: String): FunctionResponse = {

    logger.info(s"Invoking createTag with tag '$tag', message '$message', sha '$sha', owner '$owner', repo '$repo' and token '${safeToken(token)}'")

    Try {
      // val gitHub = GitHub.connectUsingOAuth(token)
      val cto = CreateTag(tag, message, sha, "commit", Tagger("Atomist Bot", "bot@atomist.com", OffsetDateTime.now()))
      val ctr = createLightweightTag(token, repo, owner, cto)
      val cr = CreateReference(s"refs/tags/${ctr.tag}", ctr.sha)
      createReference(token, repo, owner, cr)
      // val repository = gitHub.getOrganization(owner).getRepository(repo)
      // repository.createRef(s"refs/tags/${ctr.tag}", ctr.sha)
    } match {
      case Success(response) => FunctionResponse(Status.Success, Option(s"Successfully create new tag `$tag` in `$owner/$repo`"), None, JsonBodyOption(response))
      case Failure(e) => FunctionResponse(Status.Failure, Some(s"Failed to create new tag `$tag` in `$owner/$repo`"), None, StringBodyOption(e.getMessage))
    }
  }

  private def createLightweightTag(token: String, repo: String, owner: String, ct: CreateTag) =
    Http(s"$ApiUrl/repos/$owner/$repo/git/tags").postData(toJson(ct))
      .headers(getHeaders(token))
      .execute(parser = is => fromJson[CreateTagResponse](is))
      .throwError
      .body

  private def createReference(token: String, repo: String, owner: String, cr: CreateReference) =
    Http(s"$ApiUrl/repos/$owner/$repo/git/refs").postData(toJson(cr))
      .headers(getHeaders(token))
      .execute(parser = is => fromJson[Reference](is))
      .throwError
      .body
}

object CreateTagFunction {

  private case class CreateTag(tag: String,
                               message: String,
                               `object`: String,
                               `type`: String,
                               tagger: Tagger)

  private case class Tagger(name: String,
                            email: String,
                            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") date: OffsetDateTime)

  private case class CreateTagResponse(tag: String,
                                       sha: String,
                                       url: String,
                                       message: String,
                                       tagger: Tagger,
                                       `object`: ObjectResponse)

  private case class ObjectResponse(`type`: String, sha: String, url: String)

  private case class CreateReference(ref: String, sha: String)

  private case class Reference(ref: String,
                               url: String,
                               @JsonProperty("object") obj: GitHubRef)

  private case class GitHubRef(url: String, sha: String)

}