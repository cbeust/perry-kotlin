package com.beust.perry

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class UserNotFoundException(message: String): WebApplicationException(message, Response.Status.NOT_FOUND)
