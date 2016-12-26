package hello.suribot.analyze;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import hello.suribot.analyze.contracts.ContractAnalyzer;
import hello.suribot.analyze.jsonmemory.JSONMemory;
import hello.suribot.communication.ai.keys.RecastKeys;
import hello.suribot.communication.api.APIController;
import hello.suribot.communication.mbc.NodeJsMBCSender;
import hello.suribot.response.ResponseGenerator;

/**
 * Classe d'analyse des données fournies par un moteur d'intelligence (qui a analysé la demande d'un utilisateur), 
 * dans le but de fournir une réponse adaptée à la demande de l'utilisateur (par exemple: des informations bancaires, d'assurance, etc...).
 */
public class IntentsAnalyzer{
	
	private NodeJsMBCSender nextToCall;
	
	private ResponseGenerator responsegenerator;
	private APIController apicontroller;
	
	public static final String SUCCESS = "success";
	public static final String MISSINGPARAMS = "missingparams";
	public static final String CONTRAT = "contrat";
	

	public IntentsAnalyzer() {
		this.responsegenerator = new ResponseGenerator();
		this.apicontroller = new APIController();
		this.nextToCall = new NodeJsMBCSender();
	}


	/**
	 * Cette méthode analyse les données fournies par un moteur d'intelligence (qui a analysé la demande d'un utilisateur), 
	 * et permet la production d'une réponse adaptée à la demande utilisateur.
	 * @param mbc_json
	 * @param recastJson
	 * @param idUser
	 * @param firstTraitement
	 */
	public void analyzeIntents(JSONObject mbc_json, JSONObject recastJson, String idUser, boolean firstTraitement) {
		
		String contexte = null;
		JSONObject entities = null;
		boolean isChoice = false;
		try{
			
			if(firstTraitement){
				contexte = getContext(recastJson);
				entities = getEntities(recastJson);
			} else {
				contexte = recastJson.getString(JSONMemory.CONTEXTE);
				entities = recastJson.getJSONObject(JSONMemory.ENTITIES);
			}
			
			String responseToMBC = "";
			boolean demandeComprise = false;
			
			if(contexte != null && contexte.equals(CONTRAT)){
				
				//Traitement pour l'api lab-bot-api
				ContractAnalyzer analyzer = new ContractAnalyzer();

				JSONObject js = analyzer.analyze(entities, idUser);
				if(js.getBoolean(SUCCESS)){
					JSONMemory.removeLastEntities(idUser);
					String rep;
					
					try {
						rep = apicontroller.send(js.getString(ApiUrls.URITOCALL.name()));
					} catch (Exception e) {
						rep = null;
					}
					isChoice=analyzer.isChoice();
					responseToMBC = responsegenerator.generateContractUnderstoodMessage(analyzer.getCalledMethod(), isChoice, rep);
					demandeComprise = true;
					
				}else if(js.has(MISSINGPARAMS)){
					try {
						List<String> missingParams = new ArrayList<>(js.getJSONArray(MISSINGPARAMS).length());
						js.getJSONArray(MISSINGPARAMS).toList().forEach(e -> missingParams.add(e.toString()));
						if(missingParams.size()==1){
							responseToMBC = responsegenerator.generateMessageButMissOneArg(missingParams.get(0));
						} else if(missingParams.size()>1){
							responseToMBC = responsegenerator.generateMessageButMissArgs(missingParams);
						}
					} catch (Exception e){}
				
				}
			} else {
				// demande incomprise;
				// Traitement si on a d'autre contextes : transport, maps, ...
			}
			
			//On a pas réussi à traiter la demande l'utilisateur, on essaye de la compléter avec l'ancienne demande
			if(!demandeComprise){
				if(firstTraitement){
					String stringLastIntent = JSONMemory.getLastEntities(idUser);
					if(stringLastIntent==null || stringLastIntent.isEmpty()){
						// Demande incomprise et il n'y a pas d'ancienne demande en attente, 
						// donc on arrete le traitement  et envoie une erreur a SS5
						recastJson = getEntities(recastJson);
						JSONMemory.putLastEntities(idUser, recastJson.toString());
						if(contexte != null && !contexte.isEmpty()) JSONMemory.putContext(idUser, contexte);
						if(responseToMBC.isEmpty()){
							nextToCall.sendMessage(mbc_json, responsegenerator.generateNotUnderstoodMessage());
							return;
						} else {
							nextToCall.sendMessage(mbc_json, responseToMBC);
							return;
						}
					}
					//On essaye de completer l'ancienne demande présente avec les nouvelles données reçues
					JSONObject lastIntent = new JSONObject(stringLastIntent);
					JSONObject newRequest = generateNewRequestWithLastEntities(recastJson, lastIntent);
					JSONMemory.removeLastEntities(idUser);
					if(contexte != null && !contexte.isEmpty()) newRequest.put(JSONMemory.CONTEXTE, contexte);
					analyzeIntents(mbc_json, newRequest, idUser, false);
				}else{
					// Ce n'est pas le premier traitement de la demande de l'utilisateur
					
					// Demande incomprise et il n'y a pas d'ancienne demande en attente, 
					// donc on arrete le traitement  et envoie une erreur a SS5
					JSONMemory.putLastEntities(idUser, entities.toString());
					JSONMemory.putContext(idUser, contexte);
					if(responseToMBC.isEmpty()) responseToMBC = responsegenerator.generateNotUnderstoodMessage();
					nextToCall.sendMessage(mbc_json, responseToMBC);
					return;
				}
			} else { // demande comprise
				if(firstTraitement && isChoice){
					//si la demande est un choix on stocke la demande pour y ajouter eventuellement 
					entities = getEntities(recastJson);
					if(entities!=null) JSONMemory.putLastEntities(idUser, entities.toString());
					JSONMemory.putContext(idUser, contexte);
				}else{
					JSONMemory.removeLastEntities(idUser);
					JSONMemory.removeLastContext(idUser);
				}
					
				if(responseToMBC.isEmpty()){
					nextToCall.sendMessage(mbc_json, responsegenerator.generateNotUnderstoodMessage());
				} else {
					nextToCall.sendMessage(mbc_json, responseToMBC);
				}
			}
		}catch(JSONException e){
			if(firstTraitement){
				//Il y a eu une exception lors de la lecture de la recuperation du contexte,
				//on essaye de completer une ancienne demande avec les nouvelles données
				String stringLastEntities = JSONMemory.getLastEntities(idUser);
				if(stringLastEntities==null || stringLastEntities.isEmpty()){
					//Demande incomprise et il n'y a pas d'ancienne demande en attente
					//donc on arrete le traitement  et envoie une erreur a SS5
					JSONMemory.putLastEntities(idUser, recastJson.toString());
					String responseToMBC = responsegenerator.generateNotUnderstoodMessage();
					nextToCall.sendMessage(mbc_json, responseToMBC);
					return;
				}
				JSONObject lastEntities = new JSONObject(stringLastEntities);
				JSONObject newRequest = generateNewRequestWithLastEntities(recastJson, lastEntities);
				JSONMemory.removeLastEntities(idUser);
				analyzeIntents(mbc_json, newRequest, idUser, false);
			}else{
				//Demande incomprise et il n'y a pas d'ancienne demande en attente
				//donc on arrete le traitement  et envoie une erreur a SS5
				if(entities != null) JSONMemory.putLastEntities(idUser, entities.toString());
				JSONMemory.putContext(idUser, contexte);
				String responseToMBC = responsegenerator.generateNotUnderstoodMessage();
				nextToCall.sendMessage(mbc_json, responseToMBC);
			}
		}
	}
	
	/**
	 * Forme une nouvelle demande en combinant la précédente (si existante dans les fichiers ".json", voir {@link JSONMemory}) 
	 * et la nouvelle
	 * @param newDemande
	 * @param lastEntities
	 * @return
	 */
	private static JSONObject generateNewRequestWithLastEntities(JSONObject newDemande, JSONObject lastEntities){
		try{
			newDemande = getEntities(newDemande);
		}catch(JSONException e){
			return lastEntities;
		}
		for(String key: newDemande.keySet()){
			//on insere les nouvelles données de la demande à la derniere demande incomprise
			//pour essayer de la completer
			lastEntities.put(key, newDemande.get(key));
		}
		JSONObject js = new JSONObject(lastEntities.toString());
		lastEntities.put(JSONMemory.ENTITIES, js);
		return lastEntities;
	}
	
	private static String getContext(JSONObject recastJson){
		JSONObject jsonResult = null;
		if(recastJson != null){
			try{
				jsonResult = new JSONObject();
				jsonResult = (JSONObject) recastJson.get(RecastKeys.RESULTS);
				JSONArray ja = (JSONArray) jsonResult.get(RecastKeys.INTENTS);
				if(ja != null) return ja.getJSONObject(0).getString(RecastKeys.SLUG);
			}catch(JSONException e){
				return null;
			}
		}
		return null;
	}
	
	private static JSONObject getEntities(JSONObject recastJson){
		JSONObject jsonResult = new JSONObject();
		if(recastJson != null){
			jsonResult = (JSONObject) recastJson.get(RecastKeys.RESULTS);
			jsonResult = (JSONObject) jsonResult.get(RecastKeys.ENTITIES);
			return jsonResult;
		}
		return null;
	}
}