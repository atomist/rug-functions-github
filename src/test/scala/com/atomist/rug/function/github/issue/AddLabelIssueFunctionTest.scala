package com.atomist.rug.function.github.issue

import com.atomist.rug.function.github.GitHubFunctionTest
import com.atomist.rug.function.github.TestConstants.{ApiUrl, Token}
import com.atomist.rug.spi.Handlers.Status
import com.atomist.source.git.domain.Issue
import com.atomist.util.JsonUtils

class AddLabelIssueFunctionTest extends GitHubFunctionTest(Token, ApiUrl) {

  "AddLabelIssueFunction" should "add label to issue" in {
    val tempRepo = newPopulatedTemporaryRepo()
    val repo = tempRepo.name
    val owner = tempRepo.ownerName

    val issue = createIssue(repo, owner)

    val f = new AddLabelIssueFunction
    val response = f.invoke(issue.number, repo, owner, "bug", ApiUrl, Token)
    response.status shouldBe Status.Success
    val body = response.body
    body shouldBe defined
    body.get.str shouldBe defined
    val issue1 = JsonUtils.fromJson[Issue](body.get.str.get)
    val labels = issue1.labels
    labels should have size 1
    labels(0).name shouldBe "bug"
    ghs.deleteRepository(repo, owner)
  }

  it should "fail to add label to unknown issue" in {
    val tempRepo = newPopulatedTemporaryRepo()
    val repo = tempRepo.name
    val owner = tempRepo.ownerName

    val f = new ToggleIssueLabelFunction
    val issueNumber = 999
    val response = f.invoke(issueNumber, repo, owner, "bug", ApiUrl, Token)
    response.status shouldBe Status.Failure
    val body = response.msg
    body shouldBe defined
    body.get shouldEqual s"Failed to find issue `#$issueNumber` in `$owner/$repo`"

    ghs.deleteRepository(repo, owner)
  }
}
