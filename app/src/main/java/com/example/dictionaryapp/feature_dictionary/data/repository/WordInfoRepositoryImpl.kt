package com.example.dictionaryapp.feature_dictionary.data.repository

import com.example.dictionaryapp.core.util.Resource
import com.example.dictionaryapp.feature_dictionary.data.local.WordInfoDao
import com.example.dictionaryapp.feature_dictionary.data.remote.DictionaryApi
import com.example.dictionaryapp.feature_dictionary.domain.model.WordInfo
import com.example.dictionaryapp.feature_dictionary.domain.repository.WordInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class WordInfoRepositoryImpl(
    private val api: DictionaryApi,
    private val dao: WordInfoDao
) : WordInfoRepository {
    override fun getWordInfo(word: String): Flow<Resource<List<WordInfo>>> = flow {
        emit(Resource.Loading())
        // get our word infos from local database
        val wordInfos = dao.getWordInfos(word).map { it.toWordInfo() }
        emit(Resource.Loading(wordInfos))

        try {
            // get word infos from api
            val remoteWordInfos = api.getWordInfo(word)
            // delete all word infos found in our local database
            dao.deleteWordInfos(remoteWordInfos.map { it.word })
            // insert back (UPDATE) back new word infers
            dao.insertWordInfo(remoteWordInfos.map { it.toWordInfoEntity() })
        } catch (e: HttpException) {
            emit(Resource.Failure("Something went wrong", wordInfos))
        } catch (e: IOException) {
            emit(Resource.Failure("Could not reach server, check internet connectivity", wordInfos))
        }

        // Retrieve back our new word infers from local database
        val newWordInfos = dao.getWordInfos(word).map { it.toWordInfo() }
        // Emit back to viewModel
        emit(Resource.Success(newWordInfos))
    }
}