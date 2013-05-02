class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

"/user"(controller:"user"){
    action = [POST:"save"]
}
"/userservice/user"(controller:"user"){
    action = [POST:"save"]
}

"/user/$id"(controller:"user"){
    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
}
"/userservice/user/$id"(controller:"user"){
    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
}

"/useritvid/$id"(controller:"useritvid"){
    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
}
"/userservice/useritvid/$id"(controller:"useritvid"){
    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
