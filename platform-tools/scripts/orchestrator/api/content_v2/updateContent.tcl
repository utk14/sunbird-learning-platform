package require java
java::import -package java.util ArrayList List
java::import -package java.util HashMap Map Date
java::import -package java.util HashSet Set
java::import -package com.ilimi.graph.dac.model Node Relation
java::import -package com.ilimi.graph.common DateUtils

proc proc_isNotNull {value} {
	set exist false
	java::try {
		set hasValue [java::isnull $value]
		if {$hasValue == 0} {
			set exist true
		}
	} catch {Exception err} {
    	set exist false
	}
	return $exist
}

proc proc_isEmpty {value} {
	set exist false
	java::try {
		set hasValue [java::isnull $value]
		if {$hasValue == 1} {
			set exist true
		} else {
			set strValue [$value toString]
			set newStrValue [java::new String $strValue]
			set strLength [$newStrValue length]
			if {$strLength == 0} {
				set exist true
			}
		}
	} catch {Exception err} {
    	set exist true
	}
	return $exist
}

proc proc_isNotEmpty {relations} {
	set exist false
	set hasRelations [java::isnull $relations]
	if {$hasRelations == 0} {
		set relationsSize [$relations size] 
		if {$relationsSize > 0} {
			set exist true
		}
	}
	return $exist
}

proc proc_getItemSetConcepts {graphId content} {
	set concepts [java::new HashSet]
	set conceptIds [java::new ArrayList]
	set item_sets [$content get "item_sets"]
	set item_sets_null [java::isnull $item_sets]
	if {$item_sets_null == 0} {
		set item_ids [java::new ArrayList]
		set item_sets [java::cast List $item_sets]
		java::for {Object item_set} $item_sets {
			set item_set [java::cast Map $item_set]
			set item_set_id [$item_set get "identifier"]
			set setResponse [getSetMembers $graphId $item_set_id]
			set member_ids [get_resp_value $setResponse "members"]
			set member_ids_null [java::isnull $member_ids]
			if {$member_ids_null == 0} {
				set member_ids [java::cast List $member_ids]
				$item_ids addAll $member_ids
			}
		}
		set item_ids_size [$item_ids size]
		if {[$item_ids size] >= 0} {
			set item_nodes_response [getDataNodes $graphId $item_ids]
			set item_nodes [get_resp_value $item_nodes_response "node_list"]
			set item_nodes_null [java::isnull $item_nodes]
			if {$item_nodes_null == 0} {
				java::for {Node item_node} $item_nodes {
					set outRelations [java::prop $item_node "outRelations"]
					set hasOutRelations [proc_isNotEmpty $outRelations]
					if {$hasOutRelations} {
						java::for {Relation relation} $outRelations {
							if {[java::prop $relation "endNodeObjectType"] == "Concept" && [java::prop $relation "relationType"] == "associatedTo"} {
								set conceptMetadata [java::prop $relation "endNodeMetadata"]
								set conceptStatus [$conceptMetadata get "status"]
								set conceptStatus [java::new String [$conceptStatus toString]]
								set statusCheck [$conceptStatus equalsIgnoreCase "Live"]
								if {$statusCheck == 1} {
									set concept_id [java::prop $relation "endNodeId"]
									$concepts add $concept_id
								}
							}
						}
					}
				}
				$conceptIds addAll $concepts
				set conceptIds_size [$conceptIds size]
			}
		}
	}
	return $conceptIds
}

set object_null [java::isnull $content]
if {$object_null == 1} {
	set result_map [java::new HashMap]
	$result_map put "code" "ERR_CONTENT_INVALID_OBJECT"
	$result_map put "message" "Invalid Request"
	$result_map put "responseCode" [java::new Integer 400]
	set response_list [create_error_response $result_map]
	return $response_list
} else {
	set object_type "Content"
	set graph_id "domain"
	set resp_def_node [getDefinition $graph_id $object_type]
	set def_node [get_resp_value $resp_def_node "definition_node"]
	$content put "objectType" $object_type
	$content put "identifier" $content_id

	set osId_Error false
	set contentType [$content get "contentType"]
	set contentTypeNotNull [proc_isNotNull $contentType]
	set contentTypeEmpty false
	if {$contentTypeNotNull} {
		set contentTypeEmpty [proc_isEmpty $contentType]
	}
	if {!$contentTypeEmpty} {
		set osId [$content get "osId"]
		set osIdNotNull [proc_isNotNull $osId]
		set osIdEmpty false
		if {$osIdNotNull} {
			set osIdEmpty [proc_isEmpty $osId]
		}
		set osIdCheck 1
		if {$contentTypeNotNull} {
			set osIdCheck [[java::new String [$contentType toString]] equalsIgnoreCase "Asset"]
		}
		if {$osIdCheck != 1 && $osIdEmpty} {
			set osId_Error true
		}
		if {$osId_Error} {
			set result_map [java::new HashMap]
			$result_map put "code" "ERR_CONTENT_INVALID_OSID"
			$result_map put "message" "OSId cannot be empty"
			$result_map put "responseCode" [java::new Integer 400]
			set response_list [create_error_response $result_map]
			return $response_list
		} else {
			set get_node_response [getDataNode $graph_id $content_id]
			set get_node_response_error [check_response_error $get_node_response]
			if {$get_node_response_error} {
				return $get_node_response;
			} else {
				set body [$content get "body"]
				set bodyEmpty [proc_isEmpty $body]
				if {!$bodyEmpty} {
					$content put "artifactUrl" [java::null]
				}
				set graph_node [get_resp_value $get_node_response "node"]
				set metadata [java::prop $graph_node "metadata"]
				set domain_val [$metadata get "domain"]
				set domain_val_null [java::isnull $domain_val]
				if {$domain_val_null == 0} {
					set domain_val_instance [java::instanceof $domain_val {String[]}]
					if {$domain_val_instance == 0} {
						set input_domain [$content get "domain"]
						set input_domain_null [java::isnull $input_domain]
						if {$input_domain_null == 1} {
							set domain_list [java::new ArrayList]
							$domain_list add $domain_val
							$content put "domain" $domain_list
						}
					}
				}
				set status_val [$metadata get "status"]
				set status_val_str [java::new String [$status_val toString]]
				set isReviewState [$status_val_str equalsIgnoreCase "Review"]
				set input_status [$content get "status"]
				set input_status_null [java::isnull $input_status]
				set log_event 0
				if {$input_status_null == 0} {
					set input_status_str [java::new String [$input_status toString]]
					set updateToReviewState [$input_status_str equalsIgnoreCase "Review"]
					if {$updateToReviewState == 1 && $isReviewState != 1} {
						$content put "lastSubmittedOn" [java::call DateUtils format [java::new Date]]
					}
					if {![$input_status_str equals $status_val_str]} {
						set log_event 1
					}
				}
				set domain_obj [convert_to_graph_node $content $def_node]
				set create_response [updateDataNode $graph_id $content_id $domain_obj]
				set check_error [check_response_error $create_response]
				if {$check_error} {
					return $create_response
				} else {
					if {$log_event == 1} {
						$metadata putAll $content
						$metadata put "prevState" $status_val_str
						set log_response [log_content_lifecycle_event $content_id $metadata]
					}
					set concept_ids [proc_getItemSetConcepts $graph_id $content]
					if {[$concept_ids size] > 0} {
						set relationName [java::new String "associatedTo"]
						set addRelResponse [addOutRelations $graph_id $content_id $relationName $concept_ids]
						set check_error [check_response_error $addRelResponse]
					    if {$check_error} {
					        return $addRelResponse
					    }
					}
					return $create_response
				}
			}
		}
	} else {
		set result_map [java::new HashMap]
		$result_map put "code" "ERR_CONTENT_INVALID_CONTENT_TYPE"
		$result_map put "message" "Content Type cannot be empty"
		$result_map put "responseCode" [java::new Integer 400]
		set response_list [create_error_response $result_map]
		return $response_list
	}
}
