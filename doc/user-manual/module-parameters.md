## Module Parameters

### What Are Module Parameters, Anyway?

Module parameters are a form of data used to control the pipeline: either the way that Ziggy executes the processing, or the way that the algorithms themselves function. For example, module parameters can tell an algorithm module to enable or disable a particular processing step; set convergence criteria for model fitting algorithms; set the number of data points to use in a median filter; or, really, anything of that nature.

Module parameters are organized into groups known as "parameter sets." When Ziggy provides parameters to a pipeline module, it's in the form of one or more parameter sets, rather than individual parameters. There's no limit on the number of parameter sets that can be provided to a pipeline or a pipeline module, so don't feel that you need to cram a hundred parameters into one parameter set; it's probably the case that the parameters can be logically grouped into sets, and you should feel free to group them that way and then pass all the needed parameter sets to the modules that need them.

Ziggy expects its parameter set XML files to start with "pl-" (for "Parameter Library"). In the case of the sample pipeline, take a look at [config/pl-sample.xml](../sample-pipeline/config/pl-sample.xml). Note that you also don't need to confine yourself to a single parameter library file; you can have as many as you like.

### Execution Control Parameters

Ziggy provides two flavors of pre-defined parameter sets that are used by Ziggy itself to control parts of its execution: the remote parameters, that control execution on a high-performance computing system; and the task configuration parameters, which define how Ziggy subdivides data for execution in chunks.

For now, we're not going to talk further about these kinds of parameters. They'll be discussed at greater length in sections on running the pipeline. However, if you can't bear the suspense, see the following articles:

[Remote Parameters](remote-parameters.md)

[Task Configuration Parameters](task-configuration.md)

### Parameters Used by Algorithm Modules

This is more what you need to worry about as you're designing your algorithms. In `pl-sample.xml`, swim down to the last parameter set:

```xml
<parameter-set name="Algorithm Parameters">
  <parameter name="throw exception subtask_0" value="false" type="boolean"/>
  <parameter name="produce output subtask 1" value="true" type="boolean"/>
  <parameter name="dummy array parameter" value="1, 2, 3" type="intarray"/>
  <parameter name="execution pause seconds" value="5" type="int"/>
</parameter-set>
```

Each parameter set must have a unique name, in this case "Algorithm Parameters". Each parameter must have a name that is unique within the parameter set (i.e., I could have another parameter named "dummy array parameter" in a different parameter set, but obviously I can't have 2 parameters with the same name in the same set).

Each individual parameter in a set has a name, a value, and a type. The allowed types the usual ones you would expect: `boolean, byte, short, int, long, float, double, string`. The parameters can be scalar or they can be arrays: to specify an array, append `array` after the type name, as shown by the `dummy array parameter`.

Note that both the parameter set name and the parameter name can have whitespace in them. However, when the parameter sets are provided to the algorithm, as shown in the article on [Configuring a Pipeline](configuring-pipeline.md), it's done in a context that doesn't allow for whitespace. As a result, the parameter set names and parameter names will have any whitespace replaced with underscore ("_") characters. In my opinion, this is a recipe for confusion and annoying software bugs: you have two different names for parameters and/or parameter sets, but they're *almost* the same, and you need to remember exactly what the difference is and which part of the system uses each of the names. Still, if you want to do this, you can, which is why we're showing you how to do it (and then telling you not to).
