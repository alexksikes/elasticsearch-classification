Elasticsearch Classification Plugin
===================================

To build a `SNAPSHOT` version, you need to build it with Maven:

```bash
mvn clean install -DskipTests
bin/plugin install classification \
       --url file:target/releases/elasticsearch-classification-X.X.X-SNAPSHOT.zip
```

Example of Usage
----------------

```js
GET /tmdb/movies/_classify
{
  "model": "simple_naive_bayes",
  "field": "overview",
  "class": "genres.name",
  "query": {
    "match_all": {}
  },
  "text": "In the post-apocalyptic future, reigning tyrannical supercomputers teleport a cyborg assassin known as the \"Terminator\" back to 1984 to snuff Sarah Connor, whose unborn son is destined to lead insurgents against 21st century mechanical hegemony. Meanwhile, the human-resistance movement dispatches a lone warrior to safeguard Sarah. Can he stop the virtually indestructible killing machine?"
}
```

and the response:

```js
{
   "took": 1383,
   "text": "In the post-apocalyptic future, reigning tyrannical supercomputers teleport a cyborg assassin known as the \"Terminator\" back to 1984 to snuff Sarah Connor, whose unborn son is destined to lead insurgents against 21st century mechanical hegemony. Meanwhile, the human-resistance movement dispatches a lone warrior to safeguard Sarah. Can he stop the virtually indestructible killing machine?",
   "genres.name": "fiction",
   "scores": 0.49511226861962604
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
   "took": 1232,
   "text": "Marseille. 1975. Pierre Michel, jeune magistrat venu de Metz avec femme et enfants, est nommé juge du grand banditisme. Il décide de s’attaquer à la French Connection, organisation mafieuse qui exporte l’héroïne dans le monde entier.",
   "spoken_languages.name": "français",
   "scores": 0.9991379387732284
}
```

Caution: Don't use on high cardinality fields, as the process could take a long time.

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
