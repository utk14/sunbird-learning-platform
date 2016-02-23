package org.ekstep.language.mgr.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ekstep.language.common.enums.LanguageActorNames;
import org.ekstep.language.common.enums.LanguageErrorCodes;
import org.ekstep.language.common.enums.LanguageOperations;
import org.ekstep.language.common.enums.LanguageParams;
import org.ekstep.language.measures.entity.WordComplexity;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.exception.ClientException;

public class ControllerUtil extends BaseLanguageManager {

	private static Logger LOGGER = LogManager.getLogger(ControllerUtil.class.getName());

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateWordFeatures(Map item, String languageId) {
		Request langReq = getLanguageRequest(languageId, LanguageActorNames.LEXILE_MEASURES_ACTOR.name(),
				LanguageOperations.getWordFeatures.name());
		String lemma = (String) item.get("lemma");
		if (lemma == null) {
			throw new ClientException(LanguageErrorCodes.SYSTEM_ERROR.name(), "Lemma not found");
		}
		langReq.put(LanguageParams.word.name(), lemma);
		Response langRes = getLanguageResponse(langReq, LOGGER);
		if (checkError(langRes)) {
			throw new ClientException(LanguageErrorCodes.SYSTEM_ERROR.name(), langRes.getParams().getErrmsg());
		} else {
			Map<String, WordComplexity> featureMap = (Map<String, WordComplexity>) langRes
					.get(LanguageParams.word_features.name());
			if (null != featureMap && !featureMap.isEmpty()) {
				System.out.println("Word features returned for " + featureMap.size() + " words");
				for (Entry<String, WordComplexity> entry : featureMap.entrySet()) {
					WordComplexity wc = entry.getValue();
					item.put("syllableCount", wc.getCount());
					item.put("syllableNotation", wc.getNotation());
					item.put("unicodeNotation", wc.getUnicode());
					item.put("orthographic_complexity", wc.getOrthoComplexity());
					item.put("phonologic_complexity", wc.getPhonicComplexity());
					item.put("status", "Live");
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void updateFrequencyCount(Map item, String languageId) {
		String[] groupBy = new String[] { "pos", "sourceType", "source", "grade" };
		String lemma = (String) item.get("lemma");
		if (lemma == null) {
			throw new ClientException(LanguageErrorCodes.SYSTEM_ERROR.name(), "Lemma not found");
		}
		List<String> groupList = Arrays.asList(groupBy);
		Map<String, Object> indexesMap = getIndexInfo(languageId, lemma, groupList);
		Map<String, Object> wordInfoMap = getWordInfo(languageId, lemma);
		Map<String, Object> index = (Map<String, Object>) indexesMap.get(lemma);
		List<Map<String, Object>> wordInfo = (List<Map<String, Object>>) wordInfoMap.get(lemma);
		if (null != index) {
			Map<String, Object> citations = (Map<String, Object>) index.get("citations");
			if (null != citations && !citations.isEmpty()) {
				Object count = citations.get("count");
				if (null != count)
					item.put("occurrenceCount", count);
				setCountsMetadata(item, citations, "sourceType", null);
				setCountsMetadata(item, citations, "source", "source");
				setCountsMetadata(item, citations, "grade", "grade");
				setCountsMetadata(item, citations, "pos", "pos");
				addTags(item, citations, "source");
				updatePosList(item, citations);
				updateSourceTypesList(item, citations);
				updateSourcesList(item, citations);
				updateGradeList(item, citations);
			}
		}
		if (null != wordInfo && !wordInfo.isEmpty()) {
			for (Map<String, Object> info : wordInfo) {
				updateStringMetadata(item, info, "word", "variants");
				updateStringMetadata(item, info, "category", "pos_categories");
				updateStringMetadata(item, info, "gender", "genders");
				updateStringMetadata(item, info, "number", "plurality");
				updateStringMetadata(item, info, "pers", "person");
				updateStringMetadata(item, info, "grammaticalCase", "cases");
				updateStringMetadata(item, info, "inflection", "inflections");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getIndexInfo(String languageId, String word, List<String> groupList) {
		List<String> list = new ArrayList<String>();
		list.add(word);
		Request langReq = getLanguageRequest(languageId, LanguageActorNames.INDEXES_ACTOR.name(),
				LanguageOperations.getIndexInfo.name());
		langReq.put(LanguageParams.words.name(), list);
		langReq.put(LanguageParams.groupBy.name(), groupList);
		Response langRes = getLanguageResponse(langReq, LOGGER);
		if (checkError(langRes)) {
			throw new ClientException(LanguageErrorCodes.SYSTEM_ERROR.name(),
					"Unable to retreive Index Info for lemma: " + word);
		}
		Map<String, Object> map = (Map<String, Object>) langRes.get(LanguageParams.index_info.name());
		return map;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getWordInfo(String languageId, String word) {
		List<String> list = new ArrayList<String>();
		list.add(word);
		Request langReq = getLanguageRequest(languageId, LanguageActorNames.INDEXES_ACTOR.name(),
				LanguageOperations.rootWordInfo.name());
		langReq.put(LanguageParams.words.name(), list);
		Response langRes = getLanguageResponse(langReq, LOGGER);
		if (checkError(langRes)) {
			throw new ClientException(LanguageErrorCodes.SYSTEM_ERROR.name(),
					"Unable to retreive Word Info for lemma: " + word);
		}
		Map<String, Object> map = (Map<String, Object>) langRes.get(LanguageParams.root_word_info.name());
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setCountsMetadata(Map item, Map<String, Object> citations, String groupName, String prefix) {
		Map<String, Object> counts = (Map<String, Object>) citations.get(groupName);
		if (null != counts && !counts.isEmpty()) {
			for (Entry<String, Object> countEntry : counts.entrySet()) {
				String key = "count_";
				if (StringUtils.isNotBlank(prefix))
					key += (prefix.trim() + "_");
				Object value = countEntry.getValue();
				if (null != value) {
					key += countEntry.getKey().trim().replaceAll("\\s+", "_");
					item.put(key, value);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addTags(Map item, Map<String, Object> citations, String groupName) {
		Map<String, Object> sources = (Map<String, Object>) citations.get(groupName);
		if (null != sources && !sources.isEmpty()) {
			List<String> tags = (List<String>) item.get("tags");
			if (null == tags)
				tags = new ArrayList<String>();
			for (String source : sources.keySet()) {
				if (!tags.contains(source.trim()))
					tags.add(source.trim());
			}
			item.put("tags", tags);
		}
	}

	@SuppressWarnings("rawtypes")
	private void updatePosList(Map item, Map<String, Object> citations) {
		updateListMetadata(item, citations, "pos", "pos");
	}

	@SuppressWarnings("rawtypes")
	private void updateSourceTypesList(Map item, Map<String, Object> citations) {
		updateListMetadata(item, citations, "sourceType", "sourceTypes");
	}

	@SuppressWarnings("rawtypes")
	private void updateSourcesList(Map item, Map<String, Object> citations) {
		updateListMetadata(item, citations, "source", "sources");
	}

	@SuppressWarnings("rawtypes")
	private void updateGradeList(Map item, Map<String, Object> citations) {
		updateListMetadata(item, citations, "grade", "grade");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateListMetadata(Map item, Map<String, Object> citations, String indexKey, String metadataKey) {
		Map<String, Object> posList = (Map<String, Object>) citations.get(indexKey);
		if (null != posList && !posList.isEmpty()) {
			String[] arr = (String[]) item.get(metadataKey);
			List<String> sources = new ArrayList<String>();
			if (null != arr && arr.length > 0) {
				for (String str : arr)
					sources.add(str);
			}
			for (String key : posList.keySet()) {
				if (!sources.contains(key))
					sources.add(key);
			}
			item.put(metadataKey, sources);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateStringMetadata(Map item, Map<String, Object> citations, String indexKey, String metadataKey) {
		String key = (String) citations.get(indexKey);
		if (StringUtils.isNotBlank(key)) {
			Object obj = item.get(metadataKey);
			String[] arr = null;
			List<String> sources = new ArrayList<String>();
			if (null != obj) {
				if (obj instanceof String[]) {
					arr = (String[]) obj;
				} else {
					sources = (List<String>) obj;
				}
			}
			if (null != arr && arr.length > 0) {
				for (String str : arr)
					sources.add(str);
			}
			if (!sources.contains(key))
				sources.add(key);
			item.put(metadataKey, sources);
		}
	}

}
