package com.beust.perry

import javax.ws.rs.WebApplicationException

class UserNotFoundException(message: String): WebApplicationException(message)
