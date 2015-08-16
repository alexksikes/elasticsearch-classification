Elasticsearch Classification Plugin
===================================

The Elasticsearch Classification Plugin is useful for simple classification
tasks. A classifier is trained and evaluated per shard using the Lucene
classification module. The results are then combined by taking an ensemble
vote over each shard. Please see below for some examples of usage.

Example of Usage
----------------

```js
GET /tmdb/movies/_classify
{
  "model": "simple_naive_bayes",
  "field": "overview",
  "class": "genres.name.terms",
  "query": {
    "match_all": {}
  },
  "text": "In the post-apocalyptic future, reigning tyrannical supercomputers teleport a cyborg assassin known as the \"Terminator\" back to 1984 to snuff Sarah Connor, whose unborn son is destined to lead insurgents against 21st century mechanical hegemony. Meanwhile, the human-resistance movement dispatches a lone warrior to safeguard Sarah. Can he stop the virtually indestructible killing machine?"
}
```

and the response:

```js
{
   "took": 1271,
   "text": "In the post-apocalyptic future, reigning tyrannical supercomputers teleport a cyborg assassin known as the \"Terminator\" back to 1984 to snuff Sarah Connor, whose unborn son is destined to lead insurgents against 21st century mechanical hegemony. Meanwhile, the human-resistance movement dispatches a lone warrior to safeguard Sarah. Can he stop the virtually indestructible killing machine?",
   "class": "genres.name.terms",
   "scores": [
      {
         "value": "Science Fiction",
         "score": 0.9810895297181871
      },
      {
         "value": "Action",
         "score": 0.016314276011600258
      },
      {
         "value": "Adventure",
         "score": 0.0024382899765171447
      }
   ]
}
```

```js
GET /tmdb/movies/_classify
{
  "field": "overview",
  "class": "spoken_languages.name",
  "query": {
    "match_all": {}
  },
  "text": "Marseille. 1975. Pierre Michel, jeune magistrat venu de Metz avec femme et enfants, est nommé juge du grand banditisme. Il décide de s’attaquer à la French Connection, organisation mafieuse qui exporte l’héroïne dans le monde entier."
}
```

and the response:

```js
{
   "took": 1252,
   "text": "Marseille. 1975. Pierre Michel, jeune magistrat venu de Metz avec femme et enfants, est nommé juge du grand banditisme. Il décide de s’attaquer à la French Connection, organisation mafieuse qui exporte l’héroïne dans le monde entier.",
   "class": "spoken_languages.name",
   "scores": [
      {
         "value": "français",
         "score": 0.9991379387732285
      },
      {
         "value": "العربية",
         "score": 0.0008463226315128121
      },
      {
         "value": "català",
         "score": 0.0000066513950761907526
      }
   ]
}
```

Parameters
----------

The `index`, `type` specify where the model should be trained and evaluated.
The request also supports a `routing` key with a URL parameter.

The body of request has the following parameters:

Parameter | Description | Default
----------| ------------| -------
model | type of classifier to use | "simple_naive_bayes"
settings | classifier specific settings | sensible defaults
fields | an array of fields to train on | *required
class | the field holding the labels | *required
query | a query to filter which documents are used for training | match_all
text | the text that should be evaluated | *required
analyzer | analyzer to tokenize the text | analyzer at `fields[0]`

The `model` parameter can take the following values: "boolean_perceptron",
"simple_naive_bayes", "caching_naive_bayes", "knn".

The `settings` parameter is model specific. For example, a boolean Perceptron
may need a `threshold` or `batch_size`, while kNN may need the value for `k`,
`min_docs_freq`, or `min_term_freq`. This parameter uses sensible defaults as
much as possible.

The `fields` parameter could also be shorthanded with `field` if only one
field is used. In this case the array syntax is dropped.

The response is an array of scores listing all the classes guessed ordered by
decreasing `score`.

Caution
-------

Don't use on high cardinality fields, as the process could take a long time.

Installation
------------

To build a `SNAPSHOT` version, you can either build it with Maven:

```bash
mvn clean install -DskipTests
bin/plugin install classification \
       --url file:target/releases/elasticsearch-classification-X.X.X-SNAPSHOT.zip
```

Or grab the latest binary for the Elasticsearch [2.0](https://github.com/elastic/elasticsearch-classification/releases/download/v2.0.0-beta1/elasticsearch-classification-2.0.0-beta1-SNAPSHOT.zip) or for 
[1.x](https://github.com/elastic/elasticsearch-classification/releases/download/v1.0.1/elasticsearch-classification-1.0.1-SNAPSHOT.zip) and install it:

```bash
cd /path/to/elasticsearch/
bin/plugin install classification --url file:/path/to/downloads/elasticsearch-classification-X.X.X-SNAPSHOT.zip
```

License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2015 Elasticsearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
