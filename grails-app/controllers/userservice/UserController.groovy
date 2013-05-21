package userservice

import grails.converters.*
import com.gigya.*
import com.gigya.socialize.*
import com.gigya.json.*
import org.codehaus.groovy.grails.web.json.*

class UserController {
    String apiKey = "3_6GWRDj4AHlIeExmVAuKl40TRKq3skxf8ecVspKkrPWhFtaWzFe7iDtr4EvDdjv_k"
    String secretKey = "7+llpGyQOGqy0tmJ/aWhLFKqqlJqwWs9MJ5UTdFq2cU="

    def save() {


            JSONObject requestJson = request.JSON
            JSONArray changedValue = new JSONArray()
            String providerId = requestJson.get('providerId')
            String itvUserId = providerId.encodeAsMD5()
            String changeAgent = request.getHeader('X-Change-Agent');
            
            def itvJson = [itvuid: itvUserId] as JSON
            def responseJson = [providerId: providerId, itvUserId: itvUserId, href: 'http://test.api.itv.com/user/v1/' + itvUserId, method: 'GET'] as JSON
            
            String method = "accounts.setAccountInfo"
            GSRequest request = new GSRequest(apiKey, secretKey, method)
            GSObject dataJson = new GSObject(itvJson.toString())
            request.setParam("uid", providerId)
            request.setParam("data", dataJson)
            GSResponse response = request.send()

            changedValue.put('itvUserId');
            JSONObject messageJson = new JSONObject()
            messageJson.put('itvUserId', itvUserId)
            messageJson.put('href', 'http://test.api.itv.com/user/v1/' + itvUserId)
            messageJson.put('type', 'create')
            messageJson.put('changeAgent', changeAgent)
            messageJson.put('changedValue', changedValue)

            rabbitSend 'userService', 'userService', messageJson.toString()
            render text: responseJson.toString(), contentType: 'application/json', encoding:"UTF-8"
    }


    def show() { 
            String query = 'SELECT * FROM accounts WHERE data.itvuid = "' + params.id + '"'
            String confidenceFilter = request.getHeader('X-Filter');

            int sysTime = (int) (System.currentTimeMillis() / 1000L)

            String method = "accounts.search"
            GSRequest request = new GSRequest(apiKey, secretKey, method)
            request.setParam("query", query)
            request.setParam("expTime", sysTime)
            GSResponse response = request.send()
            GSObject dataResponse = response.getData()

            //Process JSON
            JSONObject returnJson = new JSONObject(dataResponse.toJsonString())
            JSONArray resultsArrayJson = returnJson.getJSONArray('results')
            JSONObject resultDataJson = resultsArrayJson.getJSONObject(0)
            JSONObject dataJson = resultDataJson.getJSONObject('data')
            JSONObject profileJson = resultDataJson.getJSONObject('profile')

            System.out.println('data=' +dataJson);
            System.out.println('profile=' +profileJson);

            //create new JSON object
            JSONObject newJson = new JSONObject()
            newJson.put('title', dataJson.get('title'))
            newJson.put('firstName', profileJson.get('firstName'))
            newJson.put('lastName', profileJson.get('lastName'))
            if (confidenceFilter == 'confidential') {
              newJson.put('email', profileJson.get('email'))
            }
            newJson.put('postcode', profileJson.get('zip'))
            newJson.put('itvUserId', dataJson.get('itvuid'))
            try {
              newJson.put('barb', dataJson.get('barb'))
            } catch (JSONException je) {
              newJson.put('barb', '')
            }
            newJson.put('subscribe', dataJson.get('subscribe'))
            newJson.put('verified', 'true')
            newJson.put('created', resultDataJson.get('createdTimestamp'))
            newJson.put('lastUpdated', resultDataJson.get('lastUpdatedTimestamp'))
            newJson.put('lastLoggedIn', resultDataJson.get('lastLoginTimestamp'))
            render text: newJson.toString(), contentType: 'application/json', encoding:"UTF-8"
    }

    def index() {
            // Do a select against gigya and get all users.
            String query = 'SELECT * FROM accounts'
            int sysTime = (int) (System.currentTimeMillis() / 1000L)
            String method = "accounts.search"
            GSRequest request = new GSRequest(apiKey, secretKey, method)
            request.setParam("query", query)
            request.setParam("expTime", sysTime)
            GSResponse response = request.send()
            GSObject dataResponse = response.getData()
            JSONObject returnJson = new JSONObject(dataResponse.toJsonString())
            JSONArray resultsArrayJson = returnJson.getJSONArray('results')


            // Here we process the results and get out the details we want.
            // We create a new json obj and stuff what we want in there
            // and then add that to a json array for returning.
            JSONArray returnArrayJson = new JSONArray()
            for (int i = 0; i < resultsArrayJson.length(); i++ ) {
              JSONObject newJson = new JSONObject()
              JSONObject resultDataJson = resultsArrayJson.getJSONObject(i)
              JSONObject dataJson = resultDataJson.getJSONObject('data')
              JSONObject profileJson = resultDataJson.getJSONObject('profile')

              newJson.put('email', profileJson.get('email'))
              try {
                newJson.put('firstName', profileJson.get('firstName'))
              } catch (JSONException je) {
                newJson.put('firstName', 'ERROR: not set yet')
              }
              try {
                newJson.put('lastName', profileJson.get('lastName'))
              } catch (JSONException je) {
                newJson.put('lastName', 'ERROR: not set yet')
              }
              try {
                newJson.put('itvUserId', dataJson.get('itvuid'))
              } catch (JSONException je) {
                newJson.put('itvUserId', 'ERROR: not set yet')
              }
              returnArrayJson.put(newJson)
            }

            // Ok lets return the results..
            render text: returnArrayJson.toString(), contentType: 'application/json', encoding:"UTF-8"
    }

    def update() {

            JSONObject requestJson = request.JSON
            String itvUserId = params.id 
            String changeAgent = request.getHeader('X-Change-Agent')
            String confidenceFilter = request.getHeader('X-Filter');
            JSONArray changedValue = new JSONArray()

            String query = 'SELECT * FROM accounts WHERE data.itvuid = "' + itvUserId + '"'

            int sysTime = (int) (System.currentTimeMillis() / 1000L)

            String method = "accounts.search"
            GSRequest request = new GSRequest(apiKey, secretKey, method)
            request.setParam("query", query)
            request.setParam("expTime", sysTime)
            GSResponse response = request.send()
            GSObject dataResponse = response.getData()

            //Let create a representation of the existing account
            //that we can use to do a compare on what sent to us.
            JSONObject returnJson = new JSONObject(dataResponse.toJsonString())
            JSONArray resultsArrayJson = returnJson.getJSONArray('results')
            JSONObject resultDataJson = resultsArrayJson.getJSONObject(0)
            JSONObject dataJson = resultDataJson.getJSONObject('data')
            JSONObject profileJson = resultDataJson.getJSONObject('profile')
            JSONObject newJson = new JSONObject()

//System.out.println('datajson=' + dataJson);

            newJson.put('title', dataJson.get('title'))
            newJson.put('firstName', profileJson.get('firstName'))
            newJson.put('lastName', profileJson.get('lastName'))
            if (confidenceFilter == 'confidential') {
              newJson.put('email', profileJson.get('email'))
            }
            newJson.put('postcode', profileJson.get('zip'))
            newJson.put('itvUserId', dataJson.get('itvuid'))
            try {
              newJson.put('barb', dataJson.get('barb'))
            } catch (JSONException je) {
              newJson.put('barb', '')
            }
            newJson.put('subscribe', dataJson.get('subscribe'))
            newJson.put('verified', 'true')

            //Ok lets update the account
            method = "accounts.setAccountInfo"
            GSRequest requestUpdate = new GSRequest(apiKey, secretKey, method)
            requestUpdate.setParam("uid", resultDataJson.get('UID'))
            GSObject dataRequestJson = new GSObject()
            GSObject profileRequestJson = new GSObject()
            if ( requestJson.get('title') != newJson.get('title') ) {
              dataRequestJson.put('title', requestJson.get('title'))
              changedValue.put('title')
            }
            if ( requestJson.get('firstName') != newJson.get('firstName') ) {
              profileRequestJson.put("firstName", requestJson.get('firstName'))
              changedValue.put('firstName')
            }
            if ( requestJson.get('lastName') != newJson.get('lastName') ) {
              profileRequestJson.put("lastName", requestJson.get('lastName'))
              changedValue.put('lastName')
            }
            if (confidenceFilter == 'confidential') {
              if ( requestJson.get('email') != newJson.get('email') ) {
                profileRequestJson.put("email", requestJson.get('email'))
                changedValue.put('email')
              }
            }
            if ( requestJson.get('postcode') != newJson.get('postcode') ) {
              profileRequestJson.put('zip', requestJson.get('postcode'))
              changedValue.put('postcode')
            }
            if ( requestJson.get('barb') != newJson.get('barb') ) {
              dataRequestJson.put("barb", requestJson.get('barb'))
              changedValue.put('barb')
            }
            if ( requestJson.get('subscribe') != newJson.get('subscribe') ) {
              dataRequestJson.put("subscribe", requestJson.get('subscribe'))
              changedValue.put('subscribe')
            }
    
            requestUpdate.setParam("data", dataRequestJson)
            requestUpdate.setParam("profile", profileRequestJson)
            GSObject params = requestUpdate.getParams();
            response = requestUpdate.send()


            //Ok let get the update account and return it.
            method = "accounts.getAccountInfo"
            request = new GSRequest(apiKey, secretKey, method)
            request.setParam("uid", resultDataJson.get('UID'))
            response = request.send()
            dataResponse = response.getData()

            //Lets create a representation of the existing account
            //to return.
            returnJson = new JSONObject(dataResponse.toJsonString())
            dataJson = returnJson.getJSONObject('data')
            profileJson = returnJson.getJSONObject('profile')
            newJson = new JSONObject()
            newJson.put('title', dataJson.get('title'))
            newJson.put('firstName', profileJson.get('firstName'))
            newJson.put('lastName', profileJson.get('lastName'))
            if (confidenceFilter == 'confidential') {
              newJson.put('email', profileJson.get('email'))
            }
            newJson.put('postcode', profileJson.get('zip'))
            newJson.put('itvUserId', dataJson.get('itvuid'))
            try {
              newJson.put('barb', dataJson.get('barb'))
            } catch (JSONException je) {
              newJson.put('barb', '')
            }
            newJson.put('subscribe', dataJson.get('subscribe'))
            newJson.put('verified', 'true')

            JSONObject messageJson = new JSONObject()
            messageJson.put('itvUserId', itvUserId)
            messageJson.put('href', 'http://test.api.itv.com/user/v1/' + itvUserId)
            messageJson.put('type', 'update')
            messageJson.put('changeAgent', changeAgent)
            messageJson.put('changedValue', changedValue)

            rabbitSend 'userService', 'userService', messageJson.toString()
            render text: newJson.toString(), contentType: 'application/json', encoding:"UTF-8"
    }

    def delete() {
            def message = [itvId: itv_id, type: 'delete'] as JSON
            rabbitSend 'userService', 'userService', message.toString()
            render text: message.toString(), contentType: 'application/json', encoding:"UTF-8"
    }
}
