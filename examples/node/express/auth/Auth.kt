package express.auth


fun isAuthenticated(user: String): Boolean {
    return user != "bob"
}