def github_archive(name, repo, commit, build_file_content = None):
    if build_file_content:
        native.new_http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
            build_file_content = build_file_content,
        )
    else:
        native.http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
        )
