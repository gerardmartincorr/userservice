package userservice

import grails.converters.*
import com.gigya.*
import com.gigya.socialize.*
import com.gigya.json.*

class UseritvidController {

    def index() { }

    def show() {

            String apiKey = "3_6GWRDj4AHlIeExmVAuKl40TRKq3skxf8ecVspKkrPWhFtaWzFe7iDtr4EvDdjv_k"
            String secretKey = "7+llpGyQOGqy0tmJ/aWhLFKqqlJqwWs9MJ5UTdFq2cU="
            String itv_id = params.id.encodeAsMD5()

            def message = [itvId: itv_id, type: 'create'] as JSON
            def itv_json = [itvuid: itv_id] as JSON

            String method = "accounts.setAccountInfo"
            GSRequest request = new GSRequest(apiKey, secretKey, method)
            GSObject data_json = new GSObject(itv_json.toString())
            request.setParam("uid", params.id)
            request.setParam("data", data_json)
            GSResponse response = request.send()

            rabbitSend 'userService', 'userService', message.toString()
            render message.toString()
    }

}
